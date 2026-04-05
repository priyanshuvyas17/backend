package com.xray.backend.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Minimal DICOM pipeline: multipart upload → {@code storage/scans/{uuid}.dcm|.dcn} → GET binary stream.
 */
@RestController
@CrossOrigin("*")
public class DicomController {

  private static final String STORAGE_SUBDIR = "storage/scans";

  /** Stored names: UUID.dcm or UUID.dcn */
  private static final Pattern STORED_FILE_PATTERN =
      Pattern.compile("(?i)^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.(dcm|dcn)$");

  @PostMapping(value = "/api/dicom/convert-and-store", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadDicom(@RequestParam("file") MultipartFile file) {
    try {
      if (file == null || file.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(Map.of("status", "error", "message", "File is required and must not be empty"));
      }

      String original = file.getOriginalFilename();
      if (original == null) {
        return ResponseEntity.badRequest()
            .body(Map.of("status", "error", "message", "Filename is required"));
      }
      String lower = original.toLowerCase(Locale.ROOT);
      if (!(lower.endsWith(".dcm") || lower.endsWith(".dcn"))) {
        return ResponseEntity.badRequest()
            .body(Map.of("status", "error", "message", "Only .dcm or .dcn files are allowed"));
      }

      String ext = lower.endsWith(".dcn") ? ".dcn" : ".dcm";
      String fileName = UUID.randomUUID() + ext;
      Path path = Paths.get(STORAGE_SUBDIR, fileName);

      System.out.println("Uploading file: " + original);
      System.out.println("Saving to: " + path.toAbsolutePath());

      Files.createDirectories(path.getParent());
      Files.write(path, file.getBytes());

      return ResponseEntity.ok(Map.of("status", "success", "fileName", fileName));
    } catch (IOException e) {
      e.printStackTrace();
      return ResponseEntity.status(500)
          .body(Map.of("status", "error", "message", "Storage failed: " + e.getMessage()));
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(500)
          .body(Map.of("status", "error", "message", e.getMessage() != null ? e.getMessage() : "Unexpected error"));
    }
  }

  /**
   * Streams raw DICOM bytes only — never JSON. No redirect to /api/dicom/.../file.
   */
  @GetMapping("/dicom/{fileName}")
  public ResponseEntity<Resource> getDicom(@PathVariable String fileName) {
    if (fileName == null || !STORED_FILE_PATTERN.matcher(fileName).matches()) {
      return ResponseEntity.badRequest().build();
    }

    Path path = Paths.get("storage/scans/" + fileName);
    Path baseDir = Paths.get(STORAGE_SUBDIR).toAbsolutePath().normalize();
    Path absolute = path.toAbsolutePath().normalize();
    if (!absolute.startsWith(baseDir)) {
      return ResponseEntity.badRequest().build();
    }

    if (!Files.exists(path)) {
      return ResponseEntity.notFound().build();
    }

    if (!Files.isRegularFile(path)) {
      return ResponseEntity.notFound().build();
    }

    System.out.println("Serving DICOM: " + absolute);

    try {
      Resource resource = new UrlResource(path.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        return ResponseEntity.notFound().build();
      }

      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType("application/dicom"))
          .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
          .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
          .body(resource);
    } catch (IOException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/api/dicom/ping")
  public String ping() {
    return "DICOM API LIVE 🚀";
  }
}
