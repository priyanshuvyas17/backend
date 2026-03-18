package com.xray.backend.model;

public record ScanInfoResponse(
    String fileName,
    String fileType,
    String patientName,
    String patientId,
    String studyDate,
    String modality,
    String bodyPartExamined
) {
}

