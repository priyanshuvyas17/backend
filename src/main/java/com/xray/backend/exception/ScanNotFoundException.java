package com.xray.backend.exception;

/**
 * Thrown when a scan (by id) cannot be found in storage.
 */
public class ScanNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ScanNotFoundException(String message) {
    super(message);
  }

  public ScanNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
