package com.xray.backend.model;

/**
 * Response returned after successful scan upload.
 */
public record ScanUploadResponse(
    String status,
    String id,
    ScanMetadata metadata,
    String message
) {
}
