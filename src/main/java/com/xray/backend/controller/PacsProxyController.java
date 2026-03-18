package com.xray.backend.controller;

import com.xray.backend.exception.PacsConnectionException;
import com.xray.backend.service.OrthancService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * PACS proxy - frontend calls backend, backend proxies to Orthanc.
 * Returns safe JSON on connection errors. Timeout protection in OrthancService.
 */
@RestController
@RequestMapping("/pacs")
@CrossOrigin("*")
public class PacsProxyController {

  private static final Logger log = LoggerFactory.getLogger(PacsProxyController.class);

  private final OrthancService orthancService;

  public PacsProxyController(OrthancService orthancService) {
    this.orthancService = orthancService;
  }

  private ResponseEntity<?> safePacsCall(String operation, SafePacsCall call) {
    try {
      return ResponseEntity.ok(call.execute());
    } catch (PacsConnectionException e) {
      log.warn("[PACS] {} failed: {}", operation, e.getMessage());
      return ResponseEntity.ok(Map.of(
          "error", true,
          "message", "PACS offline or unreachable",
          "data", List.of()));
    } catch (Exception e) {
      log.error("[PACS] {} error", operation, e);
      return ResponseEntity.ok(Map.of(
          "error", true,
          "message", "PACS error: " + (e.getMessage() != null ? e.getMessage() : "Unknown"),
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

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    try {
      boolean reachable = orthancService.isReachable();
      return ResponseEntity.ok(Map.of(
          "reachable", reachable,
          "message", reachable ? "PACS is online" : "PACS is not reachable"));
    } catch (Exception e) {
      log.warn("[PACS] status check failed: {}", e.getMessage());
      return ResponseEntity.ok(Map.of("reachable", false, "message", "PACS offline"));
    }
  }
}
