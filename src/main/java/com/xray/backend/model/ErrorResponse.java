package com.xray.backend.model;

import java.time.Instant;

public class ErrorResponse {
  private String status;
  private String message;
  private String errorCode;
  private Instant timestamp;

  public ErrorResponse(String status, String message, String errorCode) {
    this.status = status;
    this.message = message;
    this.errorCode = errorCode;
    this.timestamp = Instant.now();
  }

  public String getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public Instant getTimestamp() {
    return timestamp;
  }
}

