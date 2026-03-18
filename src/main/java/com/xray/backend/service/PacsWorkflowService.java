package com.xray.backend.service;

import com.xray.backend.dto.DicomMetadataRequest;
import com.xray.backend.exception.ImageProcessingException;
import com.xray.backend.entity.DicomImage;
import com.xray.backend.entity.Series;
import com.xray.backend.entity.Study;
import com.xray.backend.repository.DicomImageRepository;
import com.xray.backend.repository.SeriesRepository;
import org.dcm4che3.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Orchestrates the full PACS workflow: image -> DICOM conversion -> Orthanc storage -> DB update.
 * PACS is optional: if Orthanc fails, uses fallback ID and continues (developer-friendly).
 */
@Service
public class PacsWorkflowService {

  private static final Logger log = LoggerFactory.getLogger(PacsWorkflowService.class);
  private static final String FALLBACK_PREFIX = "LOCAL-";

  private final DicomConversionService dicomConversionService;
  private final OrthancService orthancService;
  private final StudyService studyService;
  private final SeriesRepository seriesRepository;
  private final DicomImageRepository dicomImageRepository;

  public PacsWorkflowService(
      DicomConversionService dicomConversionService,
      OrthancService orthancService,
      StudyService studyService,
      SeriesRepository seriesRepository,
      DicomImageRepository dicomImageRepository) {
    this.dicomConversionService = dicomConversionService;
    this.orthancService = orthancService;
    this.studyService = studyService;
    this.seriesRepository = seriesRepository;
    this.dicomImageRepository = dicomImageRepository;
  }

  /**
   * Process captured image: convert to DICOM, store in PACS (if available), update DB.
   * Never throws for missing study - auto-creates. PACS failure uses fallback ID.
   */
  @Transactional
  public String processCapturedImage(MultipartFile imageFile, DicomMetadataRequest metadata) {
    Study study = studyService.getOrCreateStudyForDicom(metadata);
    log.debug("Using study {} for patient {}", study.getStudyInstanceUid(), metadata.patientId());

    Path tempImage = null;
    try {
      tempImage = Files.createTempFile("xray-capture-", ".png");
      imageFile.transferTo(tempImage.toFile());

      Path dicomPath = dicomConversionService.convertToDicom(tempImage, metadata);

      String instanceId;
      try {
        instanceId = orthancService.storeInstance(dicomPath);
        log.info("DICOM stored in PACS, instance ID: {}", instanceId);
      } catch (Exception e) {
        instanceId = FALLBACK_PREFIX + System.currentTimeMillis();
        log.warn("PACS unavailable, using fallback instanceId {}: {}", instanceId, e.getMessage());
      }

      String seriesUid = UIDUtils.createUID();
      Series series = new Series();
      series.setStudy(study);
      series.setSeriesInstanceUid(seriesUid);
      series.setSeriesNumber(1);
      series.setModality(metadata.modality());
      series.setBodyPartExamined(metadata.bodyPartExamined());
      seriesRepository.save(series);

      long fileSize = Files.size(dicomPath);
      String fileName = dicomPath.getFileName().toString();
      DicomImage img = new DicomImage();
      img.setSeries(series);
      img.setImageUuid(UUID.randomUUID().toString());
      img.setFilePath(dicomPath.toAbsolutePath().toString());
      img.setSopInstanceUid(instanceId);
      img.setFileSize(fileSize);
      img.setWidth(0);
      img.setHeight(0);
      dicomImageRepository.save(img);

      log.info("Image processed and stored: {} (instanceId: {})", fileName, instanceId);
      return instanceId;
    } catch (IOException e) {
      throw new ImageProcessingException("Failed to process image", e);
    } finally {
      if (tempImage != null) {
        try {
          Files.deleteIfExists(tempImage);
        } catch (IOException e) {
          log.warn("Failed to delete temp file: {}", tempImage, e);
        }
      }
    }
  }
}
