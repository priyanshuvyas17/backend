package com.xray.backend.model;

/**
 * DICOM / scan metadata for viewer dashboards. Technical fields are null for non-DICOM rasters.
 */
public record ScanInfoResponse(
    String fileName,
    String fileType,
    String patientName,
    String patientId,
    String studyDate,
    String modality,
    String bodyPartExamined,
    Integer rows,
    Integer columns,
    String transferSyntaxUid,
    Boolean compressed,
    String photometricInterpretation
) {
}
