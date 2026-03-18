package com.xray.backend.controller;

import com.xray.backend.model.ScanMetadata;
import com.xray.backend.model.ScanUploadResponse;
import com.xray.backend.security.RateLimiterService;
import com.xray.backend.service.DicomService;
import com.xray.backend.service.ScanStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST API for medical scan upload, preview, and metadata.
 * - POST /upload-scan: multipart upload
 * - GET /preview/{id}: PNG preview image
 * - GET /metadata/{id}: JSON metadata
 */
@RestController
@RequestMapping
public class ScanController {

  private static final Logger log = LoggerFactory.getLogger(ScanController.class);

  private final ScanStorageService scanStorage;
  private final DicomService dicomService;
  private final RateLimiterService rateLimiter;

  public ScanController(
      ScanStorageService scanStorage,
      DicomService dicomService,
      RateLimiterService rateLimiter) {
    this.scanStorage = scanStorage;
    this.dicomService = dicomService;
    this.rateLimiter = rateLimiter;
  }

  /**
   * Upload a medical scan (.dcm, .dcn, .jpg, .png).
   * Validates file size, type, and integrity.
   */
  @PostMapping(value = "/upload-scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadScan(
      @RequestParam("file") MultipartFile file,
      HttpServletRequest request) {

    String ip = request.getRemoteAddr();
    if (!rateLimiter.allow(ip)) {
      return ResponseEntity.status(429).body(Map.of(
          "status", "error",
          "message", "Too many requests",
          "errorCode", "RATE_LIMITED"
      ));
    }

    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of(
          "status", "error",
          "message", "No file provided",
          "errorCode", "NO_FILE"
      ));
    }

    log.info("Upload scan: name={}, size={}, contentType={}",
        file.getOriginalFilename(), file.getSize(), file.getContentType());

    ScanStorageService.StoredScan stored = scanStorage.store(file);
    ScanMetadata metadata = dicomService.getMetadata(stored.id());

    return ResponseEntity.ok(new ScanUploadResponse(
        "success",
        stored.id(),
        metadata,
        "Scan uploaded successfully"
    ));
  }

  /**
   * Get preview image (PNG). For DICOM, decodes and caches; for JPG/PNG returns original.
   */
  @GetMapping("/preview/{id}")
  public ResponseEntity<org.springframework.core.io.Resource> preview(@PathVariable String id) {
    DicomService.PreviewResult result = dicomService.getPreview(id);
    String contentType = result.contentType();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .body(result.resource());
  }

  /**
   * Get scan metadata as JSON.
   */
  @GetMapping("/metadata/{id}")
  public ResponseEntity<ScanMetadata> metadata(@PathVariable String id) {
    return ResponseEntity.ok(dicomService.getMetadata(id));
  }
}
