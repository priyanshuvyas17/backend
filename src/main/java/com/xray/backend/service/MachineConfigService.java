package com.xray.backend.service;

import com.xray.backend.entity.MachineConfig;
import com.xray.backend.entity.User;
import com.xray.backend.repository.MachineConfigRepository;
import com.xray.backend.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;

@Service
public class MachineConfigService {

    private final MachineConfigRepository configRepository;
    private final UserRepository userRepository;
    private static final int DEFAULT_XRAY_PORT = 9000;

    public MachineConfigService(MachineConfigRepository configRepository, UserRepository userRepository) {
        this.configRepository = configRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MachineConfig getOrInitConfig(@NonNull Long userId, String detectedIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return configRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultConfig(user, detectedIp));
    }

    private MachineConfig createDefaultConfig(User user, String detectedIp) {
        MachineConfig config = new MachineConfig();
        config.setUser(user);
        
        String ipToSave = detectedIp;

        // If no IP detected passed, try to get local host
        if (ipToSave == null || ipToSave.isEmpty()) {
            ipToSave = getSiteLocalIpAddress();
        }
        
        config.setMachineIp(ipToSave);
        config.setMachinePort(DEFAULT_XRAY_PORT);
        config.setLocked(true); // Lock immediately as per requirement
        
        return configRepository.save(config);
    }
    
    public MachineConfig getConfig(@NonNull Long userId) {
        return configRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Config not found"));
    }

    @Transactional
    public MachineConfig updateConfigWithCurrentIp(@NonNull Long userId) {
        MachineConfig config = getOrInitConfig(userId, null);
        String currentIp = getSiteLocalIpAddress();
        
        // Prevent overwriting with loopback if we already have a valid IP
        if (currentIp.equals("127.0.0.1") || currentIp.equals("localhost") || currentIp.equals("0.0.0.0")) {
            return config;
        }
        
        if (!currentIp.equals(config.getMachineIp())) {
            config.setMachineIp(currentIp);
            return configRepository.save(config);
        }
        return config;
    }

    private String getSiteLocalIpAddress() {
        try {
            for (java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();) {
                java.net.NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                for (java.util.Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses(); addresses.hasMoreElements();) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
            // Fallback to local host if no site local address found
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
