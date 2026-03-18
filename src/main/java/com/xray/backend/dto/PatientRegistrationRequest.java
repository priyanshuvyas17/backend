package com.xray.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PatientRegistrationRequest(
    @NotBlank(message = "Patient name is required") String patientName,
    @NotBlank(message = "Patient ID is required") String patientId,
    @NotNull(message = "Age is required") Integer age,
    @NotBlank(message = "Gender is required") String gender,
    @NotBlank(message = "Body part examined is required") String bodyPartExamined,
    @NotBlank(message = "Study type is required") String studyType
) {}
