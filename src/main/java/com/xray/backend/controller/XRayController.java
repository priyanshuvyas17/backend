package com.xray.backend.controller;

import com.xray.backend.entity.MachineConfig;
import com.xray.backend.service.MachineConfigService;
import com.xray.backend.service.XRayTcpService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/xray")
public class XRayController {

    private final MachineConfigService configService;
    private final XRayTcpService tcpService;

    public XRayController(MachineConfigService configService, XRayTcpService tcpService) {
        this.configService = configService;
        this.tcpService = tcpService;
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connect(@RequestParam @NonNull Long userId) {
        try {
            MachineConfig config = configService.getConfig(userId);
            boolean connected = tcpService.checkConnection(config.getMachineIp(), config.getMachinePort());
            
            if (connected) {
                return ResponseEntity.ok(Map.of("status", "CONNECTED", "ip", config.getMachineIp(), "port", config.getMachinePort()));
            } else {
                return ResponseEntity.status(503).body(Map.of("status", "DISCONNECTED", "error", "Device unreachable"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam @NonNull Long userId) {
        // In a real system, we might maintain a persistent connection state in a singleton service
        // For this REST API, we check connectivity on demand or return cached state
        return connect(userId);
    }
}
