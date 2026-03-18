package com.xray.backend.utils;

import com.xray.backend.exception.DicomProcessingException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Utility for parsing DICOM files: reading headers, extracting metadata, and decoding pixel data.
 * Uses DCM4CHE library for DICOM support.
 */
public final class DicomDecoder {

  /** Extracted DICOM metadata. */
  public record DicomMeta(String patientId, String studyDate, String modality, int imageWidth, int imageHeight) {}

  private DicomDecoder() {
  }

  static {
    ImageIO.scanForPlugins();
  }

  /**
   * Reads DICOM header and metadata without loading pixel data.
   */
  public static Attributes readHeader(Path path) {
    try (DicomInputStream din = new DicomInputStream(path.toFile())) {
      din.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
      Attributes attrs = din.readDataset();
      if (attrs == null || attrs.isEmpty()) {
        throw new DicomProcessingException("Empty or invalid DICOM dataset: " + path.getFileName());
      }
      return attrs;
    } catch (IOException e) {
      throw new DicomProcessingException("Failed to read DICOM header: " + path.getFileName(), e);
    }
  }

  /**
   * Extracts common metadata tags from DICOM attributes.
   */
  public static DicomMeta extractMetadata(Attributes attrs) {
    return new DicomMeta(
        nullToEmpty(attrs.getString(Tag.PatientID)),
        nullToEmpty(attrs.getString(Tag.StudyDate)),
        nullToEmpty(attrs.getString(Tag.Modality)),
        attrs.getInt(Tag.Columns, 0),
        attrs.getInt(Tag.Rows, 0)
    );
  }

  /**
   * Decodes DICOM pixel data to a BufferedImage using DCM4CHE ImageIO plugins.
   */
  public static BufferedImage decodeToImage(Path path) {
    try (ImageInputStream iis = ImageIO.createImageInputStream(Files.newInputStream(path))) {
      if (iis == null) {
        throw new DicomProcessingException("Could not open image stream for: " + path.getFileName());
      }
      Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("DICOM");
      if (!readers.hasNext()) {
        throw new DicomProcessingException("No DICOM ImageReader available (dcm4che-imageio not loaded)");
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(iis, false, true);
        BufferedImage img = reader.read(0);
        if (img == null) {
          throw new DicomProcessingException("Could not decode DICOM pixel data: " + path.getFileName());
        }
        return img;
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      throw new DicomProcessingException("DICOM decode failure: " + path.getFileName(), e);
    }
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
