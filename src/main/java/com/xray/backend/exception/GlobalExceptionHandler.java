package com.xray.backend.exception;

import com.xray.backend.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(InvalidFileTypeException.class)
  public ResponseEntity<ErrorResponse> handleInvalidFileType(InvalidFileTypeException ex) {
    logger.warn("Invalid file type", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "INVALID_FILE_TYPE");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(FileSizeLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleFileSizeLimit(FileSizeLimitExceededException ex) {
    logger.warn("File size limit exceeded", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "FILE_TOO_LARGE");
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
    logger.warn("Max upload size exceeded", ex);
    ErrorResponse body = new ErrorResponse("error", "Uploaded file is too large", "FILE_TOO_LARGE");
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
  }

  @ExceptionHandler(FileStorageException.class)
  public ResponseEntity<ErrorResponse> handleStorage(FileStorageException ex) {
    logger.error("File storage error", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "STORAGE_ERROR");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  @ExceptionHandler(ScanNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleScanNotFound(ScanNotFoundException ex) {
    logger.warn("Scan not found", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "SCAN_NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler(StoredFileNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleStoredFileNotFound(StoredFileNotFoundException ex) {
    logger.warn("Stored file not found", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "FILE_NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler(DicomProcessingException.class)
  public ResponseEntity<ErrorResponse> handleDicomProcessing(DicomProcessingException ex) {
    logger.warn("DICOM processing error: {}", ex.getMessage());
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "DICOM_PROCESSING_ERROR");
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
  }

  @ExceptionHandler(com.xray.backend.exception.PacsConnectionException.class)
  public ResponseEntity<ErrorResponse> handlePacsConnection(com.xray.backend.exception.PacsConnectionException ex) {
    logger.warn("PACS connection error", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "PACS_NOT_REACHABLE");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
  }

  @ExceptionHandler(com.xray.backend.exception.DicomConversionException.class)
  public ResponseEntity<ErrorResponse> handleDicomConversion(com.xray.backend.exception.DicomConversionException ex) {
    logger.warn("DICOM conversion / validation error: {}", ex.getMessage());
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "INVALID_DICOM_OR_CONVERSION");
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
  }

  @ExceptionHandler(EmailAlreadyInUseException.class)
  public ResponseEntity<ErrorResponse> handleEmailAlreadyInUse(EmailAlreadyInUseException ex) {
    logger.warn("Registration failed: {}", ex.getMessage());
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "EMAIL_ALREADY_IN_USE");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(DeviceUnreachableException.class)
  public ResponseEntity<ErrorResponse> handleDeviceUnreachable(DeviceUnreachableException ex) {
    logger.warn("Device unreachable: {}", ex.getMessage());
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "DEVICE_UNREACHABLE");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
  }

  @ExceptionHandler(ImageProcessingException.class)
  public ResponseEntity<ErrorResponse> handleImageProcessing(ImageProcessingException ex) {
    logger.error("Image processing failed", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "IMAGE_PROCESSING_FAILED");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    String message = ex.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
        .collect(Collectors.joining("; "));
    logger.warn("Constraint violation: {}", message);
    ErrorResponse body = new ErrorResponse("error", message, "VALIDATION_FAILED");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .collect(Collectors.joining("; "));
    logger.warn("Validation failed: {}", message);
    ErrorResponse body = new ErrorResponse("error", message, "VALIDATION_FAILED");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    logger.warn("Invalid argument", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "INVALID_REQUEST");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException ex) {
    logger.warn("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
    ErrorResponse body = new ErrorResponse("error", "Not Found: " + ex.getRequestURL(), "NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    logger.error("Unexpected error", ex);
    ErrorResponse body = new ErrorResponse("error", "Internal server error", "INTERNAL_ERROR");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
