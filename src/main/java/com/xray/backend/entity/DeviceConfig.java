package com.xray.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "device_config")
public class DeviceConfig {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ip_address", nullable = false, length = 64)
  private String ipAddress;

  @Column(name = "port", nullable = false)
  private Integer port;

  @Column(name = "device_name", nullable = false, length = 255)
  private String deviceName;

  @Column(name = "active", nullable = false)
  private Boolean active;

  public Long getId() {
    return id;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getDeviceName() {
    return deviceName;
  }

  public void setDeviceName(String deviceName) {
    this.deviceName = deviceName;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }
}
