package com.xray.backend.exception;

public class PacsConnectionException extends RuntimeException {
  public PacsConnectionException(String message) {
    super(message);
  }

  public PacsConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
