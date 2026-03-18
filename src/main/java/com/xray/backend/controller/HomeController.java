package com.xray.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.time.Instant;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
            "status", "running",
            "service", "xray-backend",
            "timestamp", Instant.now().toString(),
            "message", "Welcome to X-Ray Backend API. Use /api/device-connection/recent or /system/info for data."
        );
    }
}
