package com.xray.backend.controller;

import com.xray.backend.entity.ImageMetadata;
import com.xray.backend.security.RateLimiterService;
import com.xray.backend.service.ImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class UploadController {

  private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

  private final ImageStorageService storageService;
  private final RateLimiterService rateLimiter;

  public UploadController(ImageStorageService storageService, RateLimiterService rateLimiter) {
    this.storageService = storageService;
    this.rateLimiter = rateLimiter;
  }

  /**
   * POST /api/upload - primary upload endpoint for DICOM/images (no auth required).
   * Returns fileName (stored name) for use in preview URL: GET /api/preview/{fileName}
   */
  @PostMapping(value = "/upload", consumes = "multipart/form-data")
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
    return uploadFileInternal(file, request);
  }

  @PostMapping(value = "/upload-legacy", consumes = "multipart/form-data")
  public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
    return uploadFileInternal(file, request);
  }

  private ResponseEntity<?> uploadFileInternal(MultipartFile file, HttpServletRequest request) {
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

    logger.info(
        "Legacy upload endpoint received file: name={}, size={}, contentType={}",
        file.getOriginalFilename(),
        file.getSize(),
        file.getContentType()
    );

    ImageMetadata metadata = storageService.store(file);

    return ResponseEntity.ok(Map.of(
        "status", "success",
        "fileName", metadata.getFileName(),
        "originalName", metadata.getOriginalFileName(),
        "patientName", metadata.getPatientName(),
        "modality", metadata.getModality(),
        "path", metadata.getPath(),
        "message", "File uploaded and processed successfully"
    ));
  }
}
