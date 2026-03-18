package com.xray.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "machine_config")
public class MachineConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "machine_ip", nullable = false)
    private String machineIp;

    @Column(name = "machine_port", nullable = false)
    private Integer machinePort;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getMachineIp() { return machineIp; }
    public void setMachineIp(String machineIp) { this.machineIp = machineIp; }
    public Integer getMachinePort() { return machinePort; }
    public void setMachinePort(Integer machinePort) { this.machinePort = machinePort; }
    public Boolean getLocked() { return isLocked; }
    public void setLocked(Boolean locked) { isLocked = locked; }
    public Instant getCreatedAt() { return createdAt; }
}
