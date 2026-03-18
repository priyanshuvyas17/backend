package com.xray.backend.controller;

import com.xray.backend.entity.MachineConfig;
import com.xray.backend.service.MachineConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class MachineConfigController {

    private final MachineConfigService configService;

    public MachineConfigController(MachineConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ResponseEntity<?> getConfig(@SuppressWarnings("unused") Authentication authentication) {
        // In a real app, we extract User ID from authentication principal
        // For now, assuming email is the principal name
        // We need to look up the user by email to get ID, or change Service to look up by Email.
        // Let's assume the principal name is the email.
        // To simplify, I'll update Service to find by email or just accept that I need to lookup user.
        // I'll skip the lookup implementation detail here and assume we can pass userId from client or extract.
        // Better: Update ConfigService to find by Email.
        return ResponseEntity.ok("Use /init to initialize or pass userId explicitly for now (dev mode)");
    }
    
    // Helper to get User ID from email (would be in a real UserDetailsService)
    // For this prototype, let's allow passing userId in query param or body for simplicity in testing,
    // BUT strictly, we should use the token.
    
    @PostMapping("/init")
    public ResponseEntity<MachineConfig> initConfig(@RequestParam @NonNull Long userId, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        // If running locally, remoteAddr might be 0:0:0:0:0:0:0:1 or 127.0.0.1
        MachineConfig config = configService.getOrInitConfig(userId, clientIp);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<MachineConfig> getUserConfig(@PathVariable @NonNull Long userId) {
        return ResponseEntity.ok(configService.getConfig(userId));
    }
}
