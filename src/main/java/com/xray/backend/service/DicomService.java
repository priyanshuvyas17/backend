package com.xray.backend.service;

import com.xray.backend.exception.DicomProcessingException;
import com.xray.backend.exception.ScanNotFoundException;
import com.xray.backend.model.ScanMetadata;
import com.xray.backend.utils.DicomDecoder;
import com.xray.backend.utils.FileTypeUtils;
import org.dcm4che3.data.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates DICOM parsing, metadata extraction, and preview generation.
 */
@Service
public class DicomService {

  private static final Logger log = LoggerFactory.getLogger(DicomService.class);

  private final ScanStorageService scanStorage;
  private final ConcurrentHashMap<String, Object> previewLocks = new ConcurrentHashMap<>();

  public DicomService(ScanStorageService scanStorage) {
    this.scanStorage = scanStorage;
  }

  /**
   * Returns metadata for the given scan id.
   */
  public ScanMetadata getMetadata(String id) {
    Path path = scanStorage.resolveScanPath(id);
    long fileSize;
    try {
      fileSize = Files.size(path);
    } catch (IOException e) {
      throw new ScanNotFoundException("Cannot read scan: " + id, e);
    }
    if (FileTypeUtils.isDicom(path.getFileName().toString())) {
      Attributes attrs = DicomDecoder.readHeader(path);
      DicomDecoder.DicomMeta meta = DicomDecoder.extractMetadata(attrs);
      int w = meta.imageWidth();
      int h = meta.imageHeight();
      String resolution = (w > 0 && h > 0) ? (w + "x" + h) : "N/A";
      return new ScanMetadata(
          id,
          meta.patientId(),
          meta.studyDate(),
          meta.modality(),
          resolution,
          fileSize,
          w,
          h,
          path.getFileName().toString()
      );
    }
    // Raster images: minimal metadata
    int w = 0, h = 0;
    try {
      BufferedImage img = ImageIO.read(path.toFile());
      if (img != null) {
        w = img.getWidth();
        h = img.getHeight();
      }
    } catch (IOException e) {
      log.warn("Could not read image dimensions for {}", path, e);
    }
    String resolution = (w > 0 && h > 0) ? (w + "x" + h) : "N/A";
    return new ScanMetadata(id, null, null, null, resolution, fileSize, w, h, path.getFileName().toString());
  }

  /**
   * Returns the preview image as a Resource. For DICOM, generates and caches PNG; for JPG/PNG returns original.
   */
  public PreviewResult getPreview(String id) {
    Path scanPath = scanStorage.resolveScanPath(id);
    String name = scanPath.getFileName().toString();

    if (FileTypeUtils.isPreviewableRaster(name)) {
      String ct = FileTypeUtils.isPng(name) ? "image/png" : "image/jpeg";
      return new PreviewResult(new FileSystemResource(scanPath), ct);
    }

    if (!FileTypeUtils.isDicom(name)) {
      throw new DicomProcessingException("Unsupported format for preview: " + name);
    }

    Path previewPath = scanStorage.resolvePreviewPath(id);
    if (Files.exists(previewPath)) {
      return new PreviewResult(new FileSystemResource(previewPath), "image/png");
    }

    Object lock = previewLocks.computeIfAbsent(id, k -> new Object());
    synchronized (lock) {
      try {
        if (Files.exists(previewPath)) {
          return new PreviewResult(new FileSystemResource(previewPath), "image/png");
        }
        scanStorage.ensurePreviewDir();
        BufferedImage img = DicomDecoder.decodeToImage(scanPath);
        ImageIO.write(img, "PNG", previewPath.toFile());
        return new PreviewResult(new FileSystemResource(previewPath), "image/png");
      } catch (IOException e) {
        throw new DicomProcessingException("Failed to generate preview for " + id, e);
      } finally {
        previewLocks.remove(id, lock);
      }
    }
  }

  public record PreviewResult(Resource resource, String contentType) {}
}
