package com.xray.backend.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PatientRecordDto(
    String patientName,
    String patientId,
    LocalDate studyDate,
    String modality,
    String bodyPartExamined,
    int imageCount,
    List<ImageRecordDto> images
) {
  public record ImageRecordDto(
      String fileName,
      Instant uploadDate,
      Long fileSize
  ) {}
}
