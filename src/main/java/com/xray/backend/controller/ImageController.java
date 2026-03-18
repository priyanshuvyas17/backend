package com.xray.backend.controller;

import com.xray.backend.entity.ImageMetadata;
import com.xray.backend.model.UploadResponse;
import com.xray.backend.service.ImageStorageService;
import com.xray.backend.security.RateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ImageController {

  private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

  private final ImageStorageService storageService;
  private final RateLimiterService rateLimiter;

  public ImageController(ImageStorageService storageService, RateLimiterService rateLimiter) {
    this.storageService = storageService;
    this.rateLimiter = rateLimiter;
  }

  @PostMapping("/upload")
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    if (!rateLimiter.allow(ip)) {
      return ResponseEntity.status(429).body(java.util.Map.of(
          "status", "error",
          "message", "Too many requests",
          "errorCode", "RATE_LIMITED"
      ));
    }
    if (file == null || file.isEmpty()) {
      logger.warn("Upload request received with no file or empty file");
      return ResponseEntity.badRequest().body(java.util.Map.of(
          "status", "error",
          "message", "No file provided",
          "errorCode", "NO_FILE"
      ));
    }

    logger.info(
        "Received file upload request: name={}, size={}, contentType={}",
        file.getOriginalFilename(),
        file.getSize(),
        file.getContentType()
    );

    ImageMetadata metadata = storageService.store(file);
    UploadResponse response = new UploadResponse(
        "success",
        metadata.getFileName(),
        "File uploaded successfully"
    );
    return ResponseEntity.ok(response);
  }

  @GetMapping("/images")
  public ResponseEntity<List<ImageMetadata>> listImages() {
    List<ImageMetadata> images = storageService.listAll();
    return ResponseEntity.ok(images);
  }

  @DeleteMapping("/images/{id}")
  public ResponseEntity<Void> deleteImage(@PathVariable @NonNull Long id) {
    storageService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
