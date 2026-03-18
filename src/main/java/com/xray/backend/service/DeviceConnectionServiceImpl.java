package com.xray.backend.service;

import com.xray.backend.entity.DeviceConnection;
import com.xray.backend.repository.DeviceConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class DeviceConnectionServiceImpl implements DeviceConnectionService {
    private final DeviceConnectionRepository repository;

    public DeviceConnectionServiceImpl(DeviceConnectionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public DeviceConnection logConnection(String deviceName, String ip, Integer port, String status) {
        DeviceConnection conn = new DeviceConnection();
        conn.setDeviceName(deviceName);
        conn.setIpAddress(ip);
        conn.setPort(port);
        conn.setStatus(status != null ? status : "ACTIVE");
        conn.setLastSeenAt(Instant.now());
        return repository.save(conn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceConnection> getRecentConnections() {
        return repository.findTop10ByOrderByLastSeenAtDesc();
    }
}
