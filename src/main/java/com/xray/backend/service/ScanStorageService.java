package com.xray.backend.service;

import com.xray.backend.config.XrayProperties;
import com.xray.backend.exception.FileSizeLimitExceededException;
import com.xray.backend.exception.FileStorageException;
import com.xray.backend.exception.InvalidFileTypeException;
import com.xray.backend.exception.ScanNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Manages storage of medical scans under /storage/scans with UUID filenames.
 * Supports .dcm, .dcn, .jpg, .png.
 */
@Service
public class ScanStorageService {

  private static final Logger log = LoggerFactory.getLogger(ScanStorageService.class);
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("dcm", "dcn", "jpg", "jpeg", "png");

  private final Path scanRoot;
  private final Path previewRoot;
  private final long maxFileSizeBytes;

  public ScanStorageService(XrayProperties props) {
    this.scanRoot = Paths.get(props.getScanStorageDir()).toAbsolutePath().normalize();
    this.previewRoot = scanRoot.resolve("previews").normalize();
    this.maxFileSizeBytes = props.getMaxFileSizeBytes();
    initDirectories();
  }

  /**
   * Stores uploaded file with a new UUID. Returns the id (UUID) and stored path.
   */
  public StoredScan store(MultipartFile file) {
    validateFile(file);
    String ext = resolveExtension(file.getOriginalFilename());
    String id = UUID.randomUUID().toString();
    String storedFileName = id + "." + ext;
    Path dest = scanRoot.resolve(storedFileName).normalize();
    if (!dest.startsWith(scanRoot)) {
      throw new FileStorageException("Invalid destination path");
    }
    try {
      Files.createDirectories(scanRoot);
      try (InputStream in = new BufferedInputStream(file.getInputStream())) {
        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
      }
      log.info("Stored scan: id={}, path={}", id, dest);
      return new StoredScan(id, dest, ext, file.getSize(), file.getOriginalFilename());
    } catch (IOException e) {
      log.error("Failed to store scan {}", dest, e);
      throw new FileStorageException("Failed to store scan", e);
    }
  }

  /**
   * Resolves the stored file path for a given scan id.
   */
  public Path resolveScanPath(String id) {
    validateId(id);
    for (String ext : ALLOWED_EXTENSIONS) {
      Path p = scanRoot.resolve(id + "." + ext).normalize();
      if (Files.exists(p) && p.startsWith(scanRoot)) {
        return p;
      }
    }
    throw new ScanNotFoundException("Scan not found: " + id);
  }

  /**
   * Path for cached preview PNG.
   */
  public Path resolvePreviewPath(String id) {
    validateId(id);
    Path p = previewRoot.resolve(id + ".png").normalize();
    if (!p.startsWith(previewRoot)) {
      throw new FileStorageException("Invalid preview path");
    }
    return p;
  }

  /**
   * Ensures preview directory exists.
   */
  public void ensurePreviewDir() {
    try {
      Files.createDirectories(previewRoot);
    } catch (IOException e) {
      throw new FileStorageException("Could not create preview directory", e);
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidFileTypeException("No file provided");
    }
    if (file.getSize() > maxFileSizeBytes) {
      throw new FileSizeLimitExceededException(
          "File exceeds maximum size of " + (maxFileSizeBytes / 1024 / 1024) + " MB");
    }
  }

  private String resolveExtension(String name) {
    if (name == null || name.isBlank()) {
      throw new InvalidFileTypeException("Invalid file name");
    }
    int i = name.lastIndexOf('.');
    if (i < 0 || i == name.length() - 1) {
      throw new InvalidFileTypeException("File must have a valid extension");
    }
    String ext = name.substring(i + 1).toLowerCase(Locale.ROOT);
    if (!ALLOWED_EXTENSIONS.contains(ext)) {
      throw new InvalidFileTypeException("Unsupported format: ." + ext + ". Allowed: .dcm, .dcn, .jpg, .png");
    }
    return ext;
  }

  private void validateId(String id) {
    if (id == null || id.isBlank()) {
      throw new ScanNotFoundException("Scan id is required");
    }
    if (id.contains("..") || id.contains("/") || id.contains("\\")) {
      throw new ScanNotFoundException("Invalid scan id");
    }
  }

  private void initDirectories() {
    try {
      Files.createDirectories(scanRoot);
      Files.createDirectories(previewRoot);
      log.info("Scan storage root: {}", scanRoot);
    } catch (IOException e) {
      throw new FileStorageException("Could not initialize scan storage", e);
    }
  }

  public record StoredScan(String id, Path path, String extension, long size, String originalFileName) {}
}
