package com.xray.backend.repository;

import com.xray.backend.entity.DeviceConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeviceConnectionRepository extends JpaRepository<DeviceConnection, Long> {
    List<DeviceConnection> findByStatus(String status);
    List<DeviceConnection> findTop10ByOrderByLastSeenAtDesc();
}
