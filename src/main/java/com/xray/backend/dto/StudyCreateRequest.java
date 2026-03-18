package com.xray.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StudyCreateRequest(
    @NotBlank(message = "Patient UID is required") String patientUid,
    @NotNull(message = "Study date is required") LocalDate studyDate
) {}
