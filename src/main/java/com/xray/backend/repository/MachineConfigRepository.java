package com.xray.backend.repository;

import com.xray.backend.entity.MachineConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MachineConfigRepository extends JpaRepository<MachineConfig, Long> {
    Optional<MachineConfig> findByUserId(Long userId);
}
