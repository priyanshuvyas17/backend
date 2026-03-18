package com.xray.backend.controller;

import com.xray.backend.entity.DeviceConnection;
import com.xray.backend.service.DeviceConnectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

record LogConnectionRequest(
    String deviceName,
    String ipAddress,
    Integer port,
    String status
) {}

@RestController
@RequestMapping("/api/device-connection")
public class DeviceConnectionController {
    private final DeviceConnectionService service;

    public DeviceConnectionController(DeviceConnectionService service) {
        this.service = service;
    }

    @PostMapping("/log")
    public ResponseEntity<?> logConnection(@RequestBody LogConnectionRequest req) {
        if (req.deviceName() == null || req.deviceName().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: deviceName is required");
        }
        if (req.ipAddress() == null || req.ipAddress().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: ipAddress is required");
        }
        if (req.port() == null || req.port() <= 0 || req.port() > 65535) {
            return ResponseEntity.badRequest().body("Error: valid port is required");
        }

        DeviceConnection conn = service.logConnection(
            req.deviceName(),
            req.ipAddress(),
            req.port(),
            req.status()
        );
        return ResponseEntity.ok(conn);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<DeviceConnection>> getRecent() {
        return ResponseEntity.ok(service.getRecentConnections());
    }
}
