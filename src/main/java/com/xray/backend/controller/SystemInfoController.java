package com.xray.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SystemInfoController {

    @Value("${spring.application.name:xray-backend}")
    private String applicationName;

    @GetMapping("/system/info")
    public Map<String, Object> getSystemInfo(HttpServletRequest request) {

        var runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        var runtime = Runtime.getRuntime();

        Map<String, Object> info = new HashMap<>();
        info.put("application", applicationName);

        // 🔥 LIVE NETWORK INFO (for dashboard)
        info.put("ip", request.getLocalAddr());
        info.put("port", request.getLocalPort());

        // 🔧 SYSTEM INFO
        info.put("pid", runtimeMxBean.getPid());
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("startTime", Instant.ofEpochMilli(runtimeMxBean.getStartTime()).toString());
        info.put("uptimeMs", runtimeMxBean.getUptime());
        info.put("availableProcessors", runtime.availableProcessors());
        info.put("heapUsedBytes", runtime.totalMemory() - runtime.freeMemory());
        info.put("heapMaxBytes", runtime.maxMemory());

        return info;
    }
}