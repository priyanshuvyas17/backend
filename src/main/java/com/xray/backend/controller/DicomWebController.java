package com.xray.backend.controller;

import com.xray.backend.entity.DicomImage;
import com.xray.backend.entity.Series;
import com.xray.backend.entity.Study;
import com.xray.backend.repository.DicomImageRepository;
import com.xray.backend.repository.SeriesRepository;
import com.xray.backend.repository.StudyRepository;
import com.xray.backend.security.RateLimiterService;
import com.xray.backend.service.DicomWebService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dicom-web")
public class DicomWebController {
  private final DicomWebService service;
  private final RateLimiterService rateLimiter;
  private final StudyRepository studyRepository;
  private final SeriesRepository seriesRepository;
  private final DicomImageRepository imageRepository;

  public DicomWebController(DicomWebService service,
                            RateLimiterService rateLimiter,
                            StudyRepository studyRepository,
                            SeriesRepository seriesRepository,
                            DicomImageRepository imageRepository) {
    this.service = service;
    this.rateLimiter = rateLimiter;
    this.studyRepository = studyRepository;
    this.seriesRepository = seriesRepository;
    this.imageRepository = imageRepository;
  }

  @PostMapping(value = "/studies", consumes = "multipart/form-data")
  public ResponseEntity<?> stow(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    if (!rateLimiter.allow(ip)) {
      return ResponseEntity.status(429).body(Map.of("status", "error", "message", "Too many requests", "errorCode", "RATE_LIMITED"));
    }
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "No file", "errorCode", "NO_FILE"));
    }
    DicomImage img = service.store(file);
    return ResponseEntity.ok(Map.of(
        "status", "success",
        "imageUuid", img.getImageUuid(),
        "sopInstanceUid", img.getSopInstanceUid(),
        "seriesId", img.getSeries().getSeriesId()
    ));
  }

  @GetMapping("/studies")
  public ResponseEntity<List<Study>> qidoStudies(@RequestParam(required = false) String patientName,
                                                 @RequestParam(required = false) String from,
                                                 @RequestParam(required = false) String to,
                                                 @RequestParam(required = false) String modality) {
    LocalDate f = from != null ? LocalDate.parse(from) : null;
    LocalDate t = to != null ? LocalDate.parse(to) : null;
    List<Study> list = service.searchStudies(patientName, f, t, modality);
    return ResponseEntity.ok(list);
  }

  @GetMapping("/studies/{studyUID}/series")
  public ResponseEntity<List<Series>> series(@PathVariable @NonNull String studyUID) {
    return ResponseEntity.ok(seriesRepository.findByStudy_StudyInstanceUidOrderBySeriesNumberAsc(studyUID));
  }

  @GetMapping("/studies/{studyUID}/series/{seriesUID}/instances")
  public ResponseEntity<List<DicomImage>> instances(@PathVariable @NonNull String studyUID,
                                                    @PathVariable @NonNull String seriesUID) {
    return ResponseEntity.ok(imageRepository.findBySeries_SeriesInstanceUidOrderByCreatedAtDesc(seriesUID));
  }

  @GetMapping("/studies/{studyUID}/series/{seriesUID}/instances/{sopUID}")
  public ResponseEntity<Resource> wado(@PathVariable @NonNull String studyUID,
                                       @PathVariable @NonNull String seriesUID,
                                       @PathVariable @NonNull String sopUID) {
    DicomImage img = imageRepository.findBySopInstanceUid(sopUID).orElse(null);
    if (img == null) return ResponseEntity.notFound().build();
    Path path = Path.of(img.getFilePath());
    if (!Files.exists(path)) return ResponseEntity.notFound().build();
    Resource res = new FileSystemResource(path.toFile());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sopUID + ".dcm\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(res);
  }
}
