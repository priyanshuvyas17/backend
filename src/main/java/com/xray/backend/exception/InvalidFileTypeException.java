package com.xray.backend.exception;

public class InvalidFileTypeException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  public InvalidFileTypeException(String message) {
    super(message);
  }
}
