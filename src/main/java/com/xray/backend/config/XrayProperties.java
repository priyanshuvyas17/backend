package com.xray.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xray")
public class XrayProperties {

  private String uploadDir = "uploads/";
  private String previewDir = "previews/";
  private String scanStorageDir = "storage/scans/";
  private long maxFileSizeBytes = 52428800L;

  public String getScanStorageDir() {
    return scanStorageDir;
  }

  public void setScanStorageDir(String scanStorageDir) {
    this.scanStorageDir = scanStorageDir;
  }

  public String getUploadDir() {
    return uploadDir;
  }

  public void setUploadDir(String uploadDir) {
    this.uploadDir = uploadDir;
  }

  public String getPreviewDir() {
    return previewDir;
  }

  public void setPreviewDir(String previewDir) {
    this.previewDir = previewDir;
  }

  public long getMaxFileSizeBytes() {
    return maxFileSizeBytes;
  }

  public void setMaxFileSizeBytes(long maxFileSizeBytes) {
    this.maxFileSizeBytes = maxFileSizeBytes;
  }
}

