package com.xray.backend.dto;

public record StudyCreateResponse(
    String studyUid,
    Long studyId,
    String patientUid,
    String modality,
    String studyDate,
    String message
) {}
