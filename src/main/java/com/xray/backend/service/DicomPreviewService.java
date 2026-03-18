package com.xray.backend.service;

import com.xray.backend.config.XrayProperties;
import com.xray.backend.exception.DicomProcessingException;
import com.xray.backend.repository.StoredFileRepository;
import com.xray.backend.utils.FileTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DicomPreviewService {

  private static final Logger logger = LoggerFactory.getLogger(DicomPreviewService.class);

  private final StoredFileRepository storedFileRepository;
  private final Path previewRoot;
  private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

  public DicomPreviewService(StoredFileRepository storedFileRepository, XrayProperties props) {
    this.storedFileRepository = storedFileRepository;
    this.previewRoot = Paths.get(props.getPreviewDir()).toAbsolutePath().normalize();
    initDirectory();
    // Ensures dcm4che ImageIO plugins are registered
    ImageIO.scanForPlugins();
  }

  /**
   * Returns a path to an image that can be displayed as preview.
   * - For PNG/JPG uploads: returns the original file path.
   * - For DCM/DCN uploads: returns a cached/generated PNG under previews/{uuid}.png.
   */
  public PreviewResult getOrCreatePreview(String fileName) {
    FileTypeUtils.validateSafeFileName(fileName);

    Path uploadedPath = storedFileRepository.resolveUploadedPathOrThrow(fileName);

    if (FileTypeUtils.isPreviewableRaster(fileName)) {
      String ct = FileTypeUtils.isPng(fileName) ? "image/png" : "image/jpeg";
      return new PreviewResult(uploadedPath, ct);
    }

    if (!FileTypeUtils.isDicom(fileName)) {
      throw new DicomProcessingException("Unsupported file type for preview: " + fileName);
    }

    String base = FileTypeUtils.baseName(fileName);
    Path previewPath = previewRoot.resolve(base + ".png").normalize();
    if (!previewPath.startsWith(previewRoot)) {
      throw new DicomProcessingException("Invalid preview path");
    }

    if (Files.exists(previewPath)) {
      return new PreviewResult(previewPath, "image/png");
    }

    Object lock = locks.computeIfAbsent(base, k -> new Object());
    synchronized (lock) {
      try {
        if (Files.exists(previewPath)) {
          return new PreviewResult(previewPath, "image/png");
        }
        generateDicomPreviewPng(uploadedPath, previewPath);
        return new PreviewResult(previewPath, "image/png");
      } finally {
        locks.remove(base, lock);
      }
    }
  }

  private void initDirectory() {
    try {
      Files.createDirectories(previewRoot);
      logger.info("Using preview directory {}", previewRoot);
    } catch (IOException e) {
      throw new DicomProcessingException("Could not initialize preview directory", e);
    }
  }

  private void generateDicomPreviewPng(Path dicomFile, Path previewFile) {
    try (ImageInputStream iis = ImageIO.createImageInputStream(dicomFile.toFile())) {
      if (iis == null) {
        throw new IOException("Could not open DICOM image stream");
      }
      Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("DICOM");
      if (!readers.hasNext()) {
        throw new IOException("No DICOM ImageReader available (dcm4che-imageio)");
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(iis, false, true);
        BufferedImage img = reader.read(0);
        if (img == null) {
          throw new IOException("Could not decode DICOM image");
        }
        Files.createDirectories(previewFile.getParent());
        ImageIO.write(img, "PNG", previewFile.toFile());
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      throw new DicomProcessingException("Failed to generate DICOM preview for " + dicomFile.getFileName(), e);
    }
  }

  public record PreviewResult(Path path, String contentType) {
  }
}

