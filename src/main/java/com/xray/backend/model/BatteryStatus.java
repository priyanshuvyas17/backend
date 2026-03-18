package com.xray.backend.model;

import java.time.LocalDateTime;

public class BatteryStatus {
    private int percentage; // 0-100
    private String status; // CHARGING, DISCHARGING, FULL
    private boolean plugged;
    private double voltage;
    private LocalDateTime lastUpdated;

    public BatteryStatus() {
        this.lastUpdated = LocalDateTime.now();
    }

    public BatteryStatus(int percentage, String status, boolean plugged, double voltage) {
        this.percentage = percentage;
        this.status = status;
        this.plugged = plugged;
        this.voltage = voltage;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public int getPercentage() { return percentage; }
    public void setPercentage(int percentage) { this.percentage = percentage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPlugged() { return plugged; }
    public void setPlugged(boolean plugged) { this.plugged = plugged; }

    public double getVoltage() { return voltage; }
    public void setVoltage(double voltage) { this.voltage = voltage; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
