package com.xray.backend.controller;

import com.xray.backend.entity.DeviceConfig;
import com.xray.backend.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record SaveDeviceRequest(String ipAddress, Integer port, String deviceName, Boolean active) {}
record DeviceResponse(String ipAddress, Integer port, String deviceName, Boolean active) {}

@RestController
@RequestMapping("/api/device")
public class DeviceController {
  private final DeviceService deviceService;

  public DeviceController(DeviceService deviceService) {
    this.deviceService = deviceService;
  }

  @PostMapping
  public ResponseEntity<DeviceResponse> save(@RequestBody SaveDeviceRequest req) {
    DeviceConfig e = deviceService.save(req.ipAddress(), req.port(), req.deviceName(), req.active());
    return ResponseEntity.ok(new DeviceResponse(e.getIpAddress(), e.getPort(), e.getDeviceName(), e.getActive()));
  }

  @GetMapping("/active")
  public ResponseEntity<DeviceResponse> active() {
    DeviceConfig e = deviceService.getActive();
    if (e == null) return ResponseEntity.noContent().build();
    return ResponseEntity.ok(new DeviceResponse(e.getIpAddress(), e.getPort(), e.getDeviceName(), e.getActive()));
  }
}
