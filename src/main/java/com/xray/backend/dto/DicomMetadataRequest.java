package com.xray.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record DicomMetadataRequest(
    @NotBlank String patientName,
    @NotBlank String patientId,
    @NotBlank String studyUid,
    @NotBlank String modality,
    @NotBlank String bodyPartExamined,
    LocalDate studyDate
) {}
