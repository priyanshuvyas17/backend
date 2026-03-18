package com.xray.backend.controller;

import com.xray.backend.model.BatteryStatus;
import com.xray.backend.service.BatteryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device")
public class BatteryController {

    private final BatteryService batteryService;

    public BatteryController(BatteryService batteryService) {
        this.batteryService = batteryService;
    }

    // Endpoint for the Device (or Simulator) to push updates
    @PostMapping("/battery/update")
    public ResponseEntity<String> updateBatteryStatus(@RequestBody BatteryStatus status) {
        batteryService.updateBatteryStatus(status);
        return ResponseEntity.ok("Status Updated");
    }

    // Endpoint for Frontend to get initial state
    @GetMapping("/battery")
    public ResponseEntity<BatteryStatus> getBatteryStatus() {
        return ResponseEntity.ok(batteryService.getCurrentStatus());
    }
}
