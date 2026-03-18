package com.xray.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

  /**
   * GET / - Root URL for quick browser check (Render, load balancers).
   */
  @GetMapping("/")
  public String root() {
    return "Backend is running 🚀";
  }

  /**
   * GET /health - Health check for load balancers, Render, and mobile app.
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "service", "backend",
        "timestamp", Instant.now().toString()));
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
