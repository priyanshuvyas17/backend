package com.xray.backend.controller;

import com.xray.backend.entity.MachineConfig;
import com.xray.backend.entity.User;
import com.xray.backend.repository.UserRepository;
import com.xray.backend.service.AuthService;
import com.xray.backend.service.MachineConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

record LoginRequest(
    @NotBlank(message = "Email is required") String email,
    @NotBlank(message = "Password is required") String password
) {}

record LoginResponse(String token, Long userId, String email, String name, String machineIp, Integer machinePort) {}

record RegisterRequest(
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Email is required") String email,
    @NotBlank(message = "Password is required") String password
) {}

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    private final UserRepository userRepository;
    private final MachineConfigService machineConfigService;

    public AuthController(AuthService authService, UserRepository userRepository, MachineConfigService machineConfigService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.machineConfigService = machineConfigService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        logger.info("LOGIN ATTEMPT: Email={}", req.email());
        
        String token = authService.authenticateUser(req.email(), req.password());
        
        if (token == null || token.isEmpty()) {
            logger.error("LOGIN FAILED: Token generation returned empty/null for {}", req.email());
            return ResponseEntity.status(401).body("Authentication failed: No token generated");
        }
        
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));
        long userId = user.getId();
        
        // Update/Init Machine Config with current Server IP
        MachineConfig config = machineConfigService.updateConfigWithCurrentIp(userId);
        
        logger.info("LOGIN SUCCESS: User={}, TokenLength={}, MachineIP={}", user.getEmail(), token.length(), config.getMachineIp());
        
        return ResponseEntity.ok(new LoginResponse(
            token, 
            user.getId(), 
            user.getEmail(), 
            user.getName(),
            config.getMachineIp(),
            config.getMachinePort()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        logger.info("REGISTER ATTEMPT: Email={}", req.email());
        
        User user = authService.registerUser(req.name(), req.email(), req.password());
        long userId = user.getId();
        
        // Auto-login after registration
        String token = authService.authenticateUser(req.email(), req.password());
        
        // Update/Init Machine Config with current Server IP
        MachineConfig config = machineConfigService.updateConfigWithCurrentIp(userId);
        
        logger.info("REGISTER SUCCESS: User={}, TokenGenerated=true", user.getEmail());
        
        return ResponseEntity.ok(new LoginResponse(
            token, 
            user.getId(), 
            user.getEmail(), 
            user.getName(),
            config.getMachineIp(),
            config.getMachinePort()
        ));
    }
}
