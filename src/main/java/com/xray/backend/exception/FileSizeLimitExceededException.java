package com.xray.backend.exception;

public class FileSizeLimitExceededException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  public FileSizeLimitExceededException(String message) {
    super(message);
  }
}
