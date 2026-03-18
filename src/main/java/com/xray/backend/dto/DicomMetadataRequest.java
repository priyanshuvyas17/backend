package com.xray.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DicomMetadataRequest(
    @NotBlank(message = "Patient name is required") String patientName,
    @NotBlank(message = "Patient ID is required") String patientId,
    @NotBlank(message = "Study UID is required") String studyUid,
    @NotBlank(message = "Modality is required") String modality,
    @NotBlank(message = "Body part examined is required") String bodyPartExamined,
    @NotNull(message = "Study date is required") LocalDate studyDate
) {}
