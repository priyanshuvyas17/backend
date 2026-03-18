package com.xray.backend.controller;

import com.xray.backend.service.OrthancService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pacs")
@CrossOrigin("*")
public class PacsController {

  private final OrthancService orthancService;

  public PacsController(OrthancService orthancService) {
    this.orthancService = orthancService;
  }

  /**
   * GET /api/pacs/status - Check if PACS is reachable.
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    boolean reachable = orthancService.isReachable();
    return ResponseEntity.ok(Map.of(
        "reachable", reachable,
        "message", reachable ? "PACS is online" : "PACS is not reachable"));
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
}
