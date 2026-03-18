package com.xray.backend.service;

import com.xray.backend.config.XrayProperties;
import com.xray.backend.entity.ImageMetadata;
import com.xray.backend.exception.FileSizeLimitExceededException;
import com.xray.backend.exception.FileStorageException;
import com.xray.backend.exception.InvalidFileTypeException;
import com.xray.backend.repository.ImageMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

@Service
public class ImageStorageService {

  private static final Logger logger = LoggerFactory.getLogger(ImageStorageService.class);

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("dcm", "dcn", "jpg", "jpeg", "png");

  private final ImageMetadataRepository repository;
  private final Path uploadRoot;
  private final long maxFileSizeBytes;

  public ImageStorageService(
      ImageMetadataRepository repository,
      XrayProperties xrayProperties) {
    this.repository = repository;
    this.uploadRoot = Paths.get(xrayProperties.getUploadDir()).toAbsolutePath().normalize();
    this.maxFileSizeBytes = xrayProperties.getMaxFileSizeBytes();
    initDirectory();
  }

  public ImageMetadata store(@NonNull MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new FileStorageException("No file provided for upload");
    }

    if (file.getSize() > maxFileSizeBytes) {
      throw new FileSizeLimitExceededException("File exceeds maximum allowed size of " + maxFileSizeBytes + " bytes");
    }

    String originalName = file.getOriginalFilename();
    String extension = resolveExtension(originalName);
    validateExtension(extension);

    String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    Path targetDir = uploadRoot.resolve(dateFolder).normalize();
    try {
      Files.createDirectories(targetDir);
    } catch (IOException e) {
      throw new FileStorageException("Could not create target directory", e);
    }

    String uniqueName = UUID.randomUUID().toString() + "." + extension;
    Path destination = targetDir.resolve(uniqueName).normalize();
    if (!destination.startsWith(uploadRoot)) {
      throw new FileStorageException("Invalid destination path");
    }

    try {
      Files.copy(file.getInputStream(), destination);
    } catch (IOException e) {
      logger.error("Failed to store file {}", originalName, e);
      throw new FileStorageException("Failed to store file", e);
    }

    ImageMetadata metadata = new ImageMetadata();
    metadata.setFileName(uniqueName);
    metadata.setOriginalFileName(originalName != null ? originalName : uniqueName);
    metadata.setContentType(file.getContentType());
    metadata.setSize(file.getSize());
    metadata.setPath(destination.toString());
    metadata.setUploadTime(LocalDateTime.now());

    DicomInfo dicomInfo = processDICOM(destination);
    metadata.setPatientName(dicomInfo.patientName());
    metadata.setModality(dicomInfo.modality());
    metadata.setStudyDate(dicomInfo.studyDate());

    try {
      Path previewPath = targetDir.resolve(uniqueName + ".png").normalize();
      if (ALLOWED_EXTENSIONS.contains(extension) && (extension.equals("dcm") || extension.equals("dcn"))) {
        generatePreview(destination, previewPath, metadata);
        metadata.setPreviewPath(previewPath.toString());
      }
      // For JPG/PNG, DicomPreviewService serves the original file as preview; no separate preview file needed
    } catch (Exception e) {
      logger.warn("Preview generation failed for {}", destination, e);
    }

    ImageMetadata saved = repository.save(metadata);
    logger.info("Stored image file {} as {} ({} bytes)", originalName, uniqueName, file.getSize());
    return saved;
  }

  public List<ImageMetadata> listAll() {
    return repository.findAll();
  }

  public void deleteById(@NonNull Long id) {
    ImageMetadata metadata = repository.findById(id).orElse(null);
    if (metadata == null) {
      return;
    }
    Path filePath = Paths.get(metadata.getPath());
    try {
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      logger.error("Failed to delete file from disk: {}", filePath, e);
      throw new FileStorageException("Failed to delete file from disk", e);
    }
    repository.deleteById(id);
    logger.info("Deleted image {} and removed metadata with id {}", filePath, id);
  }

  private void initDirectory() {
    try {
      Files.createDirectories(uploadRoot);
      logger.info("Using upload directory {}", uploadRoot);
    } catch (IOException e) {
      logger.error("Could not create upload directory {}", uploadRoot, e);
      throw new FileStorageException("Could not initialize upload directory", e);
    }
  }

  private String resolveExtension(String fileName) {
    if (fileName == null) {
      throw new InvalidFileTypeException("File name is missing");
    }
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot == -1 || lastDot == fileName.length() - 1) {
      throw new InvalidFileTypeException("File does not have a valid extension");
    }
    return fileName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
  }

  private void validateExtension(String extension) {
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new InvalidFileTypeException("File type ." + extension + " is not allowed");
    }
  }

  private DicomInfo processDICOM(Path path) {
    try (DicomInputStream dicomInputStream = new DicomInputStream(path.toFile())) {
      dicomInputStream.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
      Attributes attributes = dicomInputStream.readDataset();
      if (attributes == null || attributes.isEmpty()) {
        logger.warn("Empty or invalid DICOM dataset at {}", path);
        return new DicomInfo("Unknown", "Unknown", null, null, null, null);
      }
      String patientName = attributes.getString(Tag.PatientName, "Unknown");
      String modality = attributes.getString(Tag.Modality, "Unknown");
      String studyDate = attributes.getString(Tag.StudyDate, null);
      String studyUid = attributes.getString(Tag.StudyInstanceUID, null);
      String seriesUid = attributes.getString(Tag.SeriesInstanceUID, null);
      String sopUid = attributes.getString(Tag.SOPInstanceUID, null);
      return new DicomInfo(patientName, modality, studyDate, studyUid, seriesUid, sopUid);
    } catch (Exception e) {
      logger.warn("Failed to process DICOM file at {}", path, e);
      return new DicomInfo("Unknown", "Unknown", null, null, null, null);
    }
  }

  private void generatePreview(Path dicomFile, Path previewFile, ImageMetadata metadata) throws IOException {
    ImageIO.scanForPlugins();
    try (ImageInputStream iis = ImageIO.createImageInputStream(dicomFile.toFile())) {
      Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("DICOM");
      if (!readers.hasNext()) {
        throw new IOException("No DICOM ImageReader available");
      }
      ImageReader reader = readers.next();
      reader.setInput(iis, false, true);
      BufferedImage img = reader.read(0);
      if (img == null) {
        throw new IOException("Could not decode DICOM image");
      }
      metadata.setWidth(img.getWidth());
      metadata.setHeight(img.getHeight());
      Files.createDirectories(previewFile.getParent());
      ImageIO.write(img, "PNG", previewFile.toFile());
      reader.dispose();
    }
  }

  public void generatePreviewOnDemand(Long id) {
    ImageMetadata m = repository.findById(id).orElse(null);
    if (m == null) {
      return;
    }
    Path dicomPath = Paths.get(m.getPath());
    if (!Files.exists(dicomPath)) {
      return;
    }
    Path previewPath = Paths.get(m.getPreviewPath() != null ? m.getPreviewPath() : m.getPath() + ".png");
    try {
      generatePreview(dicomPath, previewPath, m);
      m.setPreviewPath(previewPath.toString());
      repository.save(m);
    } catch (IOException ignored) {
    }
  }
  private record DicomInfo(String patientName, String modality, String studyDate, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid) {
  }
}
