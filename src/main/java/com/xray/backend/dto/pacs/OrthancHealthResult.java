package com.xray.backend.dto.pacs;

/**
 * Result of an Orthanc connectivity check for {@code GET /pacs/health}.
 */
public record OrthancHealthResult(String status, String message, String orthancUrl) {
  public static OrthancHealthResult up(String orthancUrl) {
    return new OrthancHealthResult("UP", "Connected to Orthanc", orthancUrl);
  }

  public static OrthancHealthResult down(String message, String orthancUrl) {
    return new OrthancHealthResult("DOWN", message, orthancUrl);
  }
}
