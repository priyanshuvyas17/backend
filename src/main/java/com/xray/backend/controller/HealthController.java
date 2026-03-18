package com.xray.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

  /**
   * GET /health - Health check for load balancers and mobile app.
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "OK"));
  }

  /**
   * GET /test - Sample JSON for connectivity testing from phone browser.
   */
  @GetMapping("/test")
  public ResponseEntity<Map<String, Object>> test() {
    return ResponseEntity.ok(Map.of(
        "status", "OK",
        "message", "Backend is reachable",
        "timestamp", System.currentTimeMillis(),
        "service", "xray-backend"));
  }
}
