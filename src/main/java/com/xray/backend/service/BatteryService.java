package com.xray.backend.service;

import com.xray.backend.model.BatteryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BatteryService {

    private static final Logger logger = LoggerFactory.getLogger(BatteryService.class);
    private final SimpMessagingTemplate messagingTemplate;

    private BatteryStatus currentStatus;

    public BatteryService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        // Initialize with default safe state
        this.currentStatus = new BatteryStatus(100, "FULL", true, 12.0);
    }

    public void updateBatteryStatus(BatteryStatus newStatus) {
        newStatus.setLastUpdated(LocalDateTime.now());
        this.currentStatus = newStatus;

        // Medical Audit Logging
        logBatteryEvent(newStatus);

        // Safety Checks
        performSafetyChecks(newStatus);

        // Push to WebSocket
        messagingTemplate.convertAndSend("/topic/battery", newStatus);
    }

    public BatteryStatus getCurrentStatus() {
        return currentStatus;
    }

    private void logBatteryEvent(BatteryStatus status) {
        logger.info("BATTERY_UPDATE: {}% | {} | Plugged: {} | {}V", 
                status.getPercentage(), status.getStatus(), status.isPlugged(), status.getVoltage());
        
        if (status.getPercentage() < 20) {
            logger.warn("BATTERY_CRITICAL: Level below 20%! Immediate action required.");
        }
    }

    private void performSafetyChecks(BatteryStatus status) {
        if (status.getPercentage() < 10) {
            // In a real system, this would trigger a hardware interlock or disable specific beans
            logger.error("SAFETY INTERLOCK: Battery < 10%. X-Ray Exposure BLOCKED.");
        } else if (status.getPercentage() < 15) {
            logger.warn("SAFETY WARNING: Battery < 15%. X-Ray Exposure DISABLED.");
        }
    }
}
