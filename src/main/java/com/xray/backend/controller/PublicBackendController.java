package com.xray.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * Public metadata derived from {@code app.base-url} so clients can show host and port
 * without parsing long URLs in the UI (avoids ellipsis truncation on narrow layouts).
 */
@RestController
@RequestMapping("/api/public")
public class PublicBackendController {

  @Value("${app.base-url:https://backend-4ztg.onrender.com}")
  private String appBaseUrl;

  @GetMapping("/backend-connection")
  public ResponseEntity<Map<String, Object>> backendConnection() {
    String raw = appBaseUrl.trim().replaceAll("/+$", "");
    URI uri = URI.create(raw);
    String scheme = uri.getScheme() != null ? uri.getScheme() : "https";
    String host = uri.getHost() != null ? uri.getHost() : "";
    int explicitPort = uri.getPort();
    int port = explicitPort >= 0
        ? explicitPort
        : ("https".equalsIgnoreCase(scheme) ? 443 : "http".equalsIgnoreCase(scheme) ? 80 : 0);
    String portNote = explicitPort >= 0
        ? String.valueOf(explicitPort)
        : ("https".equalsIgnoreCase(scheme) ? "443 (default for HTTPS)" : "80 (default for HTTP)");

    return ResponseEntity.ok(Map.of(
        "baseUrl", raw,
        "scheme", scheme,
        "host", host,
        "port", port,
        "portDescription", portNote));
  }
}
