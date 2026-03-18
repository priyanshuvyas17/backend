package com.xray.backend.controller;

import com.xray.backend.entity.ImageMetadata;
import com.xray.backend.repository.ImageMetadataRepository;
import com.xray.backend.service.ImageStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileController {
  private final ImageMetadataRepository repository;
  private final ImageStorageService storageService;

  public FileController(ImageMetadataRepository repository, ImageStorageService storageService) {
    this.repository = repository;
    this.storageService = storageService;
  }

  @GetMapping("/file/{id}")
  public ResponseEntity<Resource> download(@PathVariable @NonNull Long id) {
    ImageMetadata m = repository.findById(id).orElse(null);
    if (m == null) return ResponseEntity.notFound().build();
    Path path = Path.of(m.getPath());
    if (!Files.exists(path)) return ResponseEntity.notFound().build();
    Resource res = new FileSystemResource(path.toFile());
    String ct = m.getContentType() != null ? m.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + m.getOriginalFileName() + "\"")
        .contentType(MediaType.parseMediaType(ct))
        .body(res);
  }

  @GetMapping("/file/{id}/metadata")
  public ResponseEntity<?> metadata(@PathVariable @NonNull Long id) {
    ImageMetadata m = repository.findById(id).orElse(null);
    if (m == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(Map.ofEntries(
        Map.entry("id", m.getId()),
        Map.entry("fileName", m.getFileName()),
        Map.entry("originalFileName", m.getOriginalFileName()),
        Map.entry("contentType", m.getContentType()),
        Map.entry("size", m.getSize()),
        Map.entry("path", m.getPath()),
        Map.entry("patientName", m.getPatientName()),
        Map.entry("modality", m.getModality()),
        Map.entry("studyDate", m.getStudyDate()),
        Map.entry("width", m.getWidth()),
        Map.entry("height", m.getHeight()),
        Map.entry("previewPath", m.getPreviewPath())
    ));
  }

  @GetMapping("/file/{id}/preview")
  public ResponseEntity<Resource> preview(@PathVariable @NonNull Long id) {
    ImageMetadata m = repository.findById(id).orElse(null);
    if (m == null) return ResponseEntity.notFound().build();
    if (m.getPreviewPath() == null || !Files.exists(Path.of(m.getPreviewPath()))) {
      try {
        storageService.generatePreviewOnDemand(id);
        m = repository.findById(id).orElse(m);
      } catch (Exception ignored) {
      }
    }
    if (m.getPreviewPath() == null || !Files.exists(Path.of(m.getPreviewPath()))) {
      return ResponseEntity.notFound().build();
    }
    Resource res = new FileSystemResource(Path.of(m.getPreviewPath()).toFile());
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(res);
  }
}
