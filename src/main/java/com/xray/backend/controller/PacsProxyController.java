package com.xray.backend.controller;

import com.xray.backend.dto.pacs.OrthancHealthResult;
import com.xray.backend.exception.PacsConnectionException;
import com.xray.backend.service.OrthancService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * PACS proxy — frontend calls the backend; the backend calls Orthanc with Basic Auth.
 * List routes return safe JSON on Orthanc errors so mobile UIs do not crash.
 */
@RestController
@RequestMapping("/pacs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PacsProxyController {

  private static final Logger log = LoggerFactory.getLogger(PacsProxyController.class);

  private final OrthancService orthancService;

  public PacsProxyController(OrthancService orthancService) {
    this.orthancService = orthancService;
  }

  /**
   * GET /pacs/health — Orthanc connectivity (always HTTP 200; body carries UP/DOWN).
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    OrthancHealthResult r = orthancService.checkHealth();
    return ResponseEntity.ok(Map.of("status", r.status(), "message", r.message()));
  }

  private ResponseEntity<?> safePacsCall(String operation, SafePacsCall call) {
    try {
      return ResponseEntity.ok(call.execute());
    } catch (PacsConnectionException e) {
      log.warn("[PACS] {} failed: {}", operation, e.getMessage());
      return ResponseEntity.ok(Map.of(
          "error", true,
          "message", e.getMessage(),
          "data", List.of()));
    } catch (Exception e) {
      log.error("[PACS] {} error", operation, e);
      return ResponseEntity.ok(Map.of(
          "error", true,
          "message", e.getMessage() != null ? e.getMessage() : "Unknown PACS error",
          "data", List.of()));
    }
  }

  @FunctionalInterface
  private interface SafePacsCall {
    Object execute();
  }

  @GetMapping("/patients")
  public ResponseEntity<?> getPatients() {
    return safePacsCall("getPatients", orthancService::getPatients);
  }

  @GetMapping("/studies")
  public ResponseEntity<?> listStudies() {
    return safePacsCall("listStudies", orthancService::getStudies);
  }

  @GetMapping("/instances")
  public ResponseEntity<?> listInstances() {
    return safePacsCall("listInstances", orthancService::getInstances);
  }

  /**
   * GET /pacs/instances/{id} — Orthanc instance metadata (clean JSON; same shape as Orthanc REST).
   */
  @GetMapping("/instances/{id}")
  public ResponseEntity<?> getInstance(@PathVariable String id) {
    return safePacsCall("getInstance", () -> orthancService.getInstance(id));
  }

  @GetMapping("/patients/{id}/studies")
  public ResponseEntity<?> getPatientStudies(@PathVariable String id) {
    return safePacsCall("getPatientStudies", () -> {
      Map<String, Object> patient = orthancService.getPatient(id);
      Object studies = patient.get("Studies");
      return studies != null ? studies : List.of();
    });
  }

  @GetMapping("/studies/{id}")
  public ResponseEntity<?> getStudy(@PathVariable String id) {
    return safePacsCall("getStudy", () -> orthancService.getStudy(id));
  }

  @GetMapping("/series/{id}")
  public ResponseEntity<?> getSeries(@PathVariable String id) {
    return safePacsCall("getSeries", () -> orthancService.getSeries(id));
  }

  /**
   * Legacy alias — prefer {@link #health()}.
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    try {
      OrthancHealthResult r = orthancService.checkHealth();
      boolean reachable = "UP".equals(r.status());
      return ResponseEntity.ok(Map.of(
          "reachable", reachable,
          "message", r.message(),
          "status", r.status()));
    } catch (Exception e) {
      log.warn("[PACS] status check failed: {}", e.getMessage());
      return ResponseEntity.ok(Map.of("reachable", false, "message", "PACS offline", "status", "DOWN"));
    }
  }
}
