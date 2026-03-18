package com.xray.backend.exception;

public class DicomConversionException extends RuntimeException {
  public DicomConversionException(String message) {
    super(message);
  }

  public DicomConversionException(String message, Throwable cause) {
    super(message, cause);
  }
}
