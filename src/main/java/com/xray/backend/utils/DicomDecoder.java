package com.xray.backend.utils;

import com.xray.backend.exception.DicomProcessingException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.io.IOException;
import java.io.InputStream;
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

  /**
   * Transfer syntax and image characteristics for routing (compressed vs uncompressed) and viewers.
   */
  public record DicomTechnicalMetadata(
      String transferSyntaxUid,
      boolean compressed,
      int rows,
      int columns,
      String photometricInterpretation
  ) {}

  /**
   * Reads transfer syntax (from file meta information) and key image tags without loading pixel bulk data.
   */
  public static DicomTechnicalMetadata readTechnicalMetadata(Path path) {
    try (DicomInputStream dis = new DicomInputStream(path.toFile())) {
      Attributes fmi = dis.getFileMetaInformation();
      String ts = fmi != null ? fmi.getString(Tag.TransferSyntaxUID, null) : null;
      dis.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
      Attributes ds = dis.readDataset();
      if (ds == null || ds.isEmpty()) {
        throw new DicomProcessingException("Empty or invalid DICOM dataset: " + path.getFileName());
      }
      int rows = ds.getInt(Tag.Rows, 0);
      int cols = ds.getInt(Tag.Columns, 0);
      String pi = ds.getString(Tag.PhotometricInterpretation, null);
      boolean compressed = isCompressedTransferSyntax(ts);
      return new DicomTechnicalMetadata(ts != null ? ts : "", compressed, rows, cols, pi);
    } catch (IOException e) {
      throw new DicomProcessingException("Failed to read DICOM technical metadata: " + path.getFileName(), e);
    }
  }

  /**
   * Uncompressed explicit/implicit VR vs compressed (JPEG, RLE, etc.).
   */
  public static boolean isCompressedTransferSyntax(String tsUid) {
    if (tsUid == null || tsUid.isEmpty()) {
      return false;
    }
    return !tsUid.equals(UID.ExplicitVRLittleEndian)
        && !tsUid.equals(UID.ImplicitVRLittleEndian)
        && !tsUid.equals(UID.ExplicitVRBigEndian);
  }

  private DicomDecoder() {
  }

  static {
    ImageIO.scanForPlugins();
  }

  /**
   * DICOM Part 10 preamble check: "DICM" at offset 128 (typical for .dcm files).
   */
  public static boolean hasDicomPreambleMagic(Path path) {
    try (InputStream in = Files.newInputStream(path)) {
      long skipped = in.skip(128);
      if (skipped < 128) {
        return false;
      }
      byte[] b = new byte[4];
      return in.read(b) == 4 && b[0] == 'D' && b[1] == 'I' && b[2] == 'C' && b[3] == 'M';
    } catch (IOException e) {
      return false;
    }
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

  /** Parse DICOM DA (StudyDate / SeriesDate) yyyyMMdd to {@link LocalDate}. */
  public static LocalDate parseDicomDate(String da) {
    if (da == null || da.length() < 8) {
      return LocalDate.now();
    }
    try {
      return LocalDate.parse(da.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
    } catch (DateTimeParseException e) {
      return LocalDate.now();
    }
  }
}
