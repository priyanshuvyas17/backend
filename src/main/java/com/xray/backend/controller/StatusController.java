package com.xray.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> status = new HashMap<>();
        // In a real scenario, check DB connection or other critical services
        // For "BUSY", we might check if a scan is running. 
        // For now, if we are here, we are ONLINE.
        status.put("status", "ONLINE"); 
        status.put("state", "ONLINE"); // Explicit state for frontend
        status.put("timestamp", LocalDateTime.now());
        status.put("service", "X-Ray Control Backend");
        status.put("version", "1.0.0");
        
        return ResponseEntity.ok(status);
    }
}
