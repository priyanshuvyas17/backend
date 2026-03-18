package com.xray.backend.service;

import com.xray.backend.entity.DeviceConnection;
import java.util.List;

public interface DeviceConnectionService {
    DeviceConnection logConnection(String deviceName, String ip, Integer port, String status);
    List<DeviceConnection> getRecentConnections();
}
