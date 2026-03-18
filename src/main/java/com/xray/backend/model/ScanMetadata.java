package com.xray.backend.model;

/**
 * DTO representing extracted metadata from a medical scan (DICOM or raster image).
 */
public record ScanMetadata(
    String id,
    String patientId,
    String studyDate,
    String modality,
    String resolution,
    long fileSize,
    int imageWidth,
    int imageHeight,
    String originalFileName
) {
}
