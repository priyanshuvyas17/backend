package com.xray.backend.controller;

import com.xray.backend.dto.pacs.OrthancHealthResult;
import com.xray.backend.service.OrthancService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pacs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PacsController {

  private final OrthancService orthancService;

  public PacsController(OrthancService orthancService) {
    this.orthancService = orthancService;
  }

  /**
   * GET /api/pacs/health — same semantics as {@code GET /pacs/health}.
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    OrthancHealthResult r = orthancService.checkHealth();
    return ResponseEntity.ok(Map.of("status", r.status(), "message", r.message()));
  }

  /**
   * GET /api/pacs/status - Check if PACS is reachable.
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    OrthancHealthResult r = orthancService.checkHealth();
    boolean reachable = "UP".equals(r.status());
    return ResponseEntity.ok(Map.of(
        "reachable", reachable,
        "message", r.message(),
        "status", r.status()));
  }

  /**
   * GET /api/pacs/patients - Get patients from Orthanc (for sync/debug).
   */
  @GetMapping("/patients")
  public ResponseEntity<List<String>> getPatients() {
    List<String> patients = orthancService.getPatients();
    return ResponseEntity.ok(patients);
  }

  /**
   * GET /api/pacs/studies - Get studies from Orthanc.
   */
  @GetMapping("/studies")
  public ResponseEntity<List<String>> getStudies() {
    List<String> studies = orthancService.getStudies();
    return ResponseEntity.ok(studies);
  }

  /**
   * GET /api/pacs/series - Get series from Orthanc.
   */
  @GetMapping("/series")
  public ResponseEntity<List<String>> getSeries() {
    List<String> series = orthancService.getSeriesList();
    return ResponseEntity.ok(series);
  }

  /**
   * GET /api/pacs/instances — instance IDs from Orthanc.
   */
  @GetMapping("/instances")
  public ResponseEntity<List<String>> getInstances() {
    return ResponseEntity.ok(orthancService.getInstances());
  }

  /**
   * GET /api/pacs/instances/{id} — full Orthanc instance JSON.
   */
  @GetMapping("/instances/{id}")
  public ResponseEntity<Map<String, Object>> getInstance(@PathVariable String id) {
    return ResponseEntity.ok(orthancService.getInstance(id));
  }
}
