package com.xray.backend.exception;

public class DicomProcessingException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DicomProcessingException(String message) {
    super(message);
  }

  public DicomProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
