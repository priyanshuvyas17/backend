package com.xray.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DeviceStatusService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceStatusService.class);
    private final SimpMessagingTemplate messagingTemplate;
    
    // Simulate device connection state
    private final AtomicBoolean isConnected = new AtomicBoolean(true);

    public DeviceStatusService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcasts a heartbeat every 5 seconds to connected clients.
     * This allows the frontend to detect if the connection is alive.
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastHeartbeat() {
        Map<String, Object> heartbeat = new HashMap<>();
        heartbeat.put("type", "HEARTBEAT");
        heartbeat.put("timestamp", LocalDateTime.now().toString());
        heartbeat.put("status", isConnected.get() ? "ONLINE" : "OFFLINE");
        heartbeat.put("message", isConnected.get() ? "Device Operational" : "Device Disconnected");

        logger.debug("Broadcasting heartbeat: {}", heartbeat);
        messagingTemplate.convertAndSend("/topic/status", heartbeat);
    }
    
    public void setDeviceConnected(boolean connected) {
        this.isConnected.set(connected);
        broadcastHeartbeat(); // Immediate update
    }
}
