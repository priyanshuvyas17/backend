package com.xray.backend.service;

import com.xray.backend.config.XrayProperties;
import com.xray.backend.dto.DicomMetadataRequest;
import com.xray.backend.exception.DicomConversionException;
import com.xray.backend.exception.ImageProcessingException;
import com.xray.backend.entity.DicomImage;
import com.xray.backend.entity.Series;
import com.xray.backend.entity.Study;
import com.xray.backend.repository.DicomImageRepository;
import com.xray.backend.repository.SeriesRepository;
import com.xray.backend.utils.DicomDecoder;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * DICOM / raster upload pipeline: validates input, stores under {@code storage/scans/} (configurable),
 * optional Orthanc, persists {@link DicomImage}.
 */
@Service
public class PacsWorkflowService {

  private static final Logger log = LoggerFactory.getLogger(PacsWorkflowService.class);

  private final DicomConversionService dicomConversionService;
  private final OrthancService orthancService;
  private final StudyService studyService;
  private final SeriesRepository seriesRepository;
  private final DicomImageRepository dicomImageRepository;
  private final XrayProperties xrayProperties;

  public PacsWorkflowService(
      DicomConversionService dicomConversionService,
      OrthancService orthancService,
      StudyService studyService,
      SeriesRepository seriesRepository,
      DicomImageRepository dicomImageRepository,
      XrayProperties xrayProperties) {
    this.dicomConversionService = dicomConversionService;
    this.orthancService = orthancService;
    this.studyService = studyService;
    this.seriesRepository = seriesRepository;
    this.dicomImageRepository = dicomImageRepository;
    this.xrayProperties = xrayProperties;
  }

  /**
   * Process upload: native DICOM Part 10 is copied and validated; raster images are converted to DICOM.
   */
  @Transactional
  public DicomUploadResult processCapturedImage(MultipartFile imageFile, DicomMetadataRequest metadata) {
    String originalName = imageFile.getOriginalFilename() != null ? imageFile.getOriginalFilename() : "upload";
    long size = imageFile.getSize();
    log.info("Uploading file: name={}, size={} bytes, contentType={}", originalName, size, imageFile.getContentType());

    if (imageFile.isEmpty()) {
      throw new ImageProcessingException("No file content");
    }

    Path stagedUpload = null;
    try {
      String suffix = guessSuffix(originalName);
      stagedUpload = Files.createTempFile("dicom-pipeline-", suffix);
      log.info("Saving multipart to temp: {}", stagedUpload.toAbsolutePath());
      imageFile.transferTo(stagedUpload.toFile());
      log.debug("Temp file size on disk: {} bytes", Files.size(stagedUpload));

      boolean treatAsDicom =
          isDicomFilename(originalName)
              || DicomDecoder.hasDicomPreambleMagic(stagedUpload)
              || isReadableDicomDataset(stagedUpload);

      DicomMetadataRequest effectiveMeta = metadata;

      if (treatAsDicom) {
        log.info("Detected DICOM Part 10 / dataset — skipping raster conversion");
        Attributes attrs;
        try {
          attrs = DicomDecoder.readHeader(stagedUpload);
        } catch (Exception e) {
          log.error("Invalid DICOM dataset: {}", e.getMessage(), e);
          throw new DicomConversionException("Invalid or corrupted DICOM file: " + e.getMessage(), e);
        }
        effectiveMeta = mergeFromDicomAttributes(metadata, attrs);
      } else {
        log.info("Raster upload — converting to DICOM Secondary Capture");
        try {
          Path converted = dicomConversionService.convertToDicom(stagedUpload, metadata);
          Files.deleteIfExists(stagedUpload);
          stagedUpload = converted;
        } catch (DicomConversionException e) {
          log.error("DICOM conversion failed: {}", e.getMessage(), e);
          throw e;
        }
      }

      Study study = studyService.getOrCreateStudyForDicom(effectiveMeta);
      log.info("Using study {} for patient {}", study.getStudyInstanceUid(), effectiveMeta.patientId());

      String fileName = UUID.randomUUID() + ".dcm";
      Path storageRoot = Paths.get(xrayProperties.getScanStorageDir()).toAbsolutePath().normalize();
      Path targetPath = storageRoot.resolve(fileName);
      Files.createDirectories(targetPath.getParent());
      log.info("Saving DICOM to destination: {}", targetPath);

      try {
        Files.copy(stagedUpload, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("File saved successfully: {} ({} bytes)", targetPath, Files.size(targetPath));
      } finally {
        try {
          Files.deleteIfExists(stagedUpload);
        } catch (IOException cleanup) {
          log.warn("Could not delete staged file {}", stagedUpload, cleanup);
        }
        stagedUpload = null;
      }

      try {
        orthancService.storeInstance(targetPath);
        log.info("Stored in Orthanc PACS");
      } catch (Exception e) {
        log.warn("PACS not reachable, continuing with local storage only: {}", e.getMessage());
      }

      String seriesUid = UIDUtils.createUID();
      Series series = new Series();
      series.setStudy(study);
      series.setSeriesInstanceUid(seriesUid);
      series.setSeriesNumber(1);
      series.setModality(effectiveMeta.modality());
      series.setBodyPartExamined(effectiveMeta.bodyPartExamined());
      seriesRepository.save(series);

      long fileSize = Files.size(targetPath);
      DicomImage img = new DicomImage();
      img.setSeries(series);
      img.setImageUuid(UUID.randomUUID().toString());
      img.setFilePath(targetPath.toAbsolutePath().toString());
      img.setSopInstanceUid(fileName);
      img.setFileSize(fileSize);
      img.setWidth(0);
      img.setHeight(0);
      dicomImageRepository.save(img);

      log.info("DICOM pipeline complete: fileName={}, imageUuid={}", fileName, img.getImageUuid());
      return new DicomUploadResult(fileName, img.getImageUuid(), img.getImageId());
    } catch (IOException e) {
      log.error("Storage IO failure: {}", e.getMessage(), e);
      throw new ImageProcessingException("Failed to store DICOM: " + e.getMessage(), e);
    } finally {
      if (stagedUpload != null) {
        try {
          Files.deleteIfExists(stagedUpload);
        } catch (IOException e) {
          log.warn("Failed to delete staged file: {}", stagedUpload, e);
        }
      }
    }
  }

  private static boolean isReadableDicomDataset(Path path) {
    try {
      DicomDecoder.readHeader(path);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean isDicomFilename(String name) {
    if (name == null) {
      return false;
    }
    String lower = name.toLowerCase(Locale.ROOT);
    return lower.endsWith(".dcm") || lower.endsWith(".dcn");
  }

  private static String guessSuffix(String originalName) {
    if (originalName != null) {
      String lower = originalName.toLowerCase(Locale.ROOT);
      if (lower.endsWith(".dcm") || lower.endsWith(".dcn")) {
        return lower.endsWith(".dcm") ? ".dcm" : ".dcn";
      }
      if (lower.endsWith(".png")) {
        return ".png";
      }
      if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
        return ".jpg";
      }
    }
    return ".bin";
  }

  private static DicomMetadataRequest mergeFromDicomAttributes(DicomMetadataRequest form, Attributes attrs) {
    String studyUid =
        (form.studyUid() != null && !form.studyUid().isBlank())
            ? form.studyUid().trim()
            : attrs.getString(Tag.StudyInstanceUID, UIDUtils.createUID());
    String patientId =
        (form.patientId() != null && !form.patientId().isBlank() && !"UNKNOWN".equalsIgnoreCase(form.patientId()))
            ? form.patientId()
            : nz(attrs.getString(Tag.PatientID), "UNKNOWN");
    String patientName =
        (form.patientName() != null && !form.patientName().isBlank() && !"Unknown".equalsIgnoreCase(form.patientName()))
            ? form.patientName()
            : nz(attrs.getString(Tag.PatientName), "Unknown");
    String modality =
        (form.modality() != null && !form.modality().isBlank() && !"OT".equalsIgnoreCase(form.modality()))
            ? form.modality()
            : nz(attrs.getString(Tag.Modality), "OT");
    String bodyPart =
        (form.bodyPartExamined() != null
                && !form.bodyPartExamined().isBlank()
                && !"UNKNOWN".equalsIgnoreCase(form.bodyPartExamined()))
            ? form.bodyPartExamined()
            : nz(attrs.getString(Tag.BodyPartExamined), "UNKNOWN");
    var studyDate =
        form.studyDate() != null
            ? form.studyDate()
            : DicomDecoder.parseDicomDate(attrs.getString(Tag.StudyDate));
    return new DicomMetadataRequest(patientName, patientId, studyUid, modality, bodyPart, studyDate);
  }

  private static String nz(String v, String fallback) {
    return (v != null && !v.isBlank()) ? v.trim() : fallback;
  }

  public record DicomUploadResult(String fileName, String imageUuid, Long imageId) {}
}
