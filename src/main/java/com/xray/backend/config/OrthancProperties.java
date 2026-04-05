package com.xray.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Orthanc REST API (default port 8042). Use Basic Auth (default orthanc/orthanc).
 * <p>
 * On cloud (e.g. Render), {@code localhost} refers to the container — set {@code ORTHANC_URL}
 * to a publicly reachable Orthanc instance.
 */
@Component
@ConfigurationProperties(prefix = "orthanc")
public class OrthancProperties {

  /**
   * Base URL without trailing slash, e.g. {@code http://localhost:8042} or {@code https://orthanc.example.com}.
   */
  private String url = "http://localhost:8042";

  private String username = "orthanc";
  private String password = "orthanc";

  /** Connect and read timeout in seconds. */
  private int timeoutSeconds = 30;

  /** When false, Orthanc calls are skipped (health reports DOWN). */
  private boolean enabled = true;

  /** Retries for transient network failures (connection reset, timeouts). */
  private int maxRetries = 2;

  /** Base delay between retries in ms (multiplied by attempt index). */
  private long retryDelayMs = 400L;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public long getRetryDelayMs() {
    return retryDelayMs;
  }

  public void setRetryDelayMs(long retryDelayMs) {
    this.retryDelayMs = retryDelayMs;
  }

  /** True when integration is on and a non-blank URL is set. */
  public boolean isConfigured() {
    return enabled && url != null && !url.isBlank();
  }

  /** Normalized base URL (no trailing slash) for display and logging (no credentials). */
  public String normalizedBaseUrl() {
    if (url == null || url.isBlank()) {
      return "";
    }
    return url.trim().replaceAll("/+$", "");
  }
}
