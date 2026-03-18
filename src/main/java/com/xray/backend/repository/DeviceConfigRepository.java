package com.xray.backend.repository;

import com.xray.backend.entity.DeviceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeviceConfigRepository extends JpaRepository<DeviceConfig, Long> {
  Optional<DeviceConfig> findTopByActiveTrueOrderByIdDesc();
}
