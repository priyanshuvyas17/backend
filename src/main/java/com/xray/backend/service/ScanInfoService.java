package com.xray.backend.service;

import com.xray.backend.exception.DicomProcessingException;
import com.xray.backend.model.ScanInfoResponse;
import com.xray.backend.repository.StoredFileRepository;
import com.xray.backend.utils.FileTypeUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class ScanInfoService {

  private final StoredFileRepository storedFileRepository;

  public ScanInfoService(StoredFileRepository storedFileRepository) {
    this.storedFileRepository = storedFileRepository;
  }

  public ScanInfoResponse getScanInfo(String fileName) {
    FileTypeUtils.validateSafeFileName(fileName);
    Path uploadedPath = storedFileRepository.resolveUploadedPathOrThrow(fileName);

    if (FileTypeUtils.isPreviewableRaster(fileName)) {
      return new ScanInfoResponse(
          fileName,
          FileTypeUtils.extension(fileName),
          null,
          null,
          null,
          null,
          null
      );
    }

    if (!FileTypeUtils.isDicom(fileName)) {
      throw new DicomProcessingException("Unsupported file type for scan-info: " + fileName);
    }

    try (DicomInputStream din = new DicomInputStream(uploadedPath.toFile())) {
      din.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
      Attributes a = din.readDataset();
      if (a == null || a.isEmpty()) {
        throw new DicomProcessingException("Empty or invalid DICOM dataset: " + fileName);
      }

      String patientName = a.getString(Tag.PatientName, null);
      String patientId = a.getString(Tag.PatientID, null);
      String studyDate = a.getString(Tag.StudyDate, null);
      String modality = a.getString(Tag.Modality, null);
      String bodyPart = a.getString(Tag.BodyPartExamined, null);

      return new ScanInfoResponse(
          fileName,
          FileTypeUtils.extension(fileName),
          patientName,
          patientId,
          studyDate,
          modality,
          bodyPart
      );
    } catch (Exception e) {
      throw new DicomProcessingException("Failed to extract DICOM metadata for " + fileName, e);
    }
  }
}

