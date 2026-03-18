package com.xray.backend.model;

public class UploadResponse {
  private String status;
  private String fileName;
  private String message;

  public UploadResponse(String status, String fileName, String message) {
    this.status = status;
    this.fileName = fileName;
    this.message = message;
  }

  public String getStatus() {
    return status;
  }

  public String getFileName() {
    return fileName;
  }

  public String getMessage() {
    return message;
  }
}
