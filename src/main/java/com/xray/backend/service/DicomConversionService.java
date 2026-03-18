package com.xray.backend.service;

import com.xray.backend.dto.DicomMetadataRequest;
import com.xray.backend.exception.DicomConversionException;
import org.dcm4che3.data.*;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Converts captured images (PNG, JPEG) to DICOM format with patient/study metadata.
 * Uses dcm4che for DICOM creation.
 */
@Service
public class DicomConversionService {

  private static final Logger log = LoggerFactory.getLogger(DicomConversionService.class);
  private static final String MODALITY_XRAY = "XR";
  private static final String SOP_CLASS_UID = UID.SecondaryCaptureImageStorage;

  @Value("${xray.upload-dir:uploads}")
  private String uploadDir;

  /**
   * Converts an image file to DICOM with the given metadata.
   *
   * @param imagePath Path to the source image (PNG, JPEG, etc.)
   * @param metadata  Patient/study metadata to embed
   * @return Path to the created DICOM file
   */
  public Path convertToDicom(Path imagePath, DicomMetadataRequest metadata) {
    try {
      BufferedImage img = ImageIO.read(imagePath.toFile());
      if (img == null) {
        throw new DicomConversionException("Could not read image: " + imagePath.getFileName());
      }
      return convertToDicom(img, metadata);
    } catch (IOException e) {
      throw new DicomConversionException("Failed to read image: " + imagePath.getFileName(), e);
    }
  }

  /**
   * Converts a BufferedImage to DICOM with the given metadata.
   */
  public Path convertToDicom(BufferedImage img, DicomMetadataRequest metadata) {
    try {
      Attributes attrs = createDicomDataset(img, metadata);
      Path outputPath = Path.of(uploadDir).resolve(UUID.randomUUID() + ".dcm");
      Files.createDirectories(outputPath.getParent());

      try (DicomOutputStream dos = new DicomOutputStream(outputPath.toFile())) {
        Attributes fmi = attrs.createFileMetaInformation(UID.ExplicitVRLittleEndian);
        dos.writeDataset(fmi, attrs);
      }
      log.info("DICOM file created: {}", outputPath.getFileName());
      return outputPath;
    } catch (IOException e) {
      throw new DicomConversionException("Failed to write DICOM file", e);
    }
  }

  private Attributes createDicomDataset(BufferedImage img, DicomMetadataRequest metadata) {
    Attributes attrs = new Attributes();
    ElementDictionary dict = ElementDictionary.getStandardElementDictionary();

    // Generate UIDs
    String studyUid = metadata.studyUid();
    String seriesUid = UIDUtils.createUID();
    String sopInstanceUid = UIDUtils.createUID();

    // Patient
    attrs.setString(Tag.PatientName, VR.PN, metadata.patientName());
    attrs.setString(Tag.PatientID, VR.LO, metadata.patientId());
    attrs.setString(Tag.PatientBirthDate, VR.DA, "");

    // Study
    attrs.setString(Tag.StudyInstanceUID, VR.UI, studyUid);
    attrs.setString(Tag.StudyDate, VR.DA, formatDate(metadata.studyDate()));
    attrs.setString(Tag.StudyTime, VR.TM, LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
    attrs.setString(Tag.StudyDescription, VR.LO, "X-Ray " + metadata.bodyPartExamined());

    // Series
    attrs.setString(Tag.SeriesInstanceUID, VR.UI, seriesUid);
    attrs.setString(Tag.SeriesNumber, VR.IS, "1");
    attrs.setString(Tag.Modality, VR.CS, metadata.modality());
    attrs.setString(Tag.BodyPartExamined, VR.CS, metadata.bodyPartExamined());

    // Instance
    attrs.setString(Tag.SOPInstanceUID, VR.UI, sopInstanceUid);
    attrs.setString(Tag.SOPClassUID, VR.UI, SOP_CLASS_UID);
    attrs.setString(Tag.InstanceNumber, VR.IS, "1");

    // Image
    int rows = img.getHeight();
    int cols = img.getWidth();
    attrs.setInt(Tag.Rows, VR.US, rows);
    attrs.setInt(Tag.Columns, VR.US, cols);
    attrs.setInt(Tag.SamplesPerPixel, VR.US, 1);
    attrs.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");
    attrs.setInt(Tag.BitsAllocated, VR.US, 16);
    attrs.setInt(Tag.BitsStored, VR.US, 12);
    attrs.setInt(Tag.HighBit, VR.US, 11);
    attrs.setInt(Tag.PixelRepresentation, VR.US, 0);

    // Content date/time
    attrs.setString(Tag.ContentDate, VR.DA, LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    attrs.setString(Tag.ContentTime, VR.TM, LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));

    // Pixel data - convert BufferedImage to grayscale 16-bit
    byte[] pixelData = convertToGrayscale16(img);
    attrs.setBytes(Tag.PixelData, VR.OW, pixelData);

    return attrs;
  }

  private byte[] convertToGrayscale16(BufferedImage img) {
    int w = img.getWidth();
    int h = img.getHeight();
    int[] rgb = img.getRGB(0, 0, w, h, null, 0, w);
    byte[] out = new byte[w * h * 2]; // 16-bit = 2 bytes per pixel

    for (int i = 0; i < rgb.length; i++) {
      int r = (rgb[i] >> 16) & 0xFF;
      int g = (rgb[i] >> 8) & 0xFF;
      int b = rgb[i] & 0xFF;
      int gray = (r + g + b) / 3;
      int gray16 = (gray << 4) & 0xFFF; // Scale to 12-bit
      out[i * 2] = (byte) (gray16 & 0xFF);
      out[i * 2 + 1] = (byte) ((gray16 >> 8) & 0xFF);
    }
    return out;
  }

  private String formatDate(LocalDate date) {
    return date != null ? date.format(DateTimeFormatter.BASIC_ISO_DATE) : LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
  }
}
