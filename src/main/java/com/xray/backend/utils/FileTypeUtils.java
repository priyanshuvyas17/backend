package com.xray.backend.utils;

import java.util.Locale;

public final class FileTypeUtils {
  private FileTypeUtils() {
  }

  public static String extension(String fileName) {
    if (fileName == null) return "";
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) return "";
    return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  public static String baseName(String fileName) {
    if (fileName == null) return "";
    int dot = fileName.lastIndexOf('.');
    if (dot < 0) return fileName;
    return fileName.substring(0, dot);
  }

  public static boolean isDicom(String fileName) {
    String ext = extension(fileName);
    return ext.equals("dcm") || ext.equals("dcn");
  }

  public static boolean isPng(String fileName) {
    return extension(fileName).equals("png");
  }

  public static boolean isJpgOrJpeg(String fileName) {
    String ext = extension(fileName);
    return ext.equals("jpg") || ext.equals("jpeg");
  }

  public static boolean isPreviewableRaster(String fileName) {
    return isPng(fileName) || isJpgOrJpeg(fileName);
  }

  public static void validateSafeFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      throw new IllegalArgumentException("fileName is required");
    }
    // Prevent path traversal and separators
    if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\") || fileName.contains("\0")) {
      throw new IllegalArgumentException("Invalid fileName");
    }
  }
}

