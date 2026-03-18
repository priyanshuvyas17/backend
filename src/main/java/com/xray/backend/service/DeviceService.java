package com.xray.backend.service;

import com.xray.backend.entity.DeviceConfig;
import com.xray.backend.repository.DeviceConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {
  private final DeviceConfigRepository repo;

  public DeviceService(DeviceConfigRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public DeviceConfig save(String ip, Integer port, String name, Boolean activeFlag) {
    DeviceConfig e = new DeviceConfig();
    e.setIpAddress(ip);
    e.setPort(port);
    e.setDeviceName(name);
    e.setActive(activeFlag != null && activeFlag);
    return repo.save(e);
  }

  @Transactional(readOnly = true)
  public DeviceConfig getActive() {
    return repo.findTopByActiveTrueOrderByIdDesc().orElse(null);
  }
}
