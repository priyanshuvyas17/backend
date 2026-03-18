package com.xray.backend.exception;

import com.xray.backend.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
    logger.error("DICOM processing error", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "DICOM_PROCESSING_ERROR");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  @ExceptionHandler(com.xray.backend.exception.PacsConnectionException.class)
  public ResponseEntity<ErrorResponse> handlePacsConnection(com.xray.backend.exception.PacsConnectionException ex) {
    logger.warn("PACS connection error", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "PACS_NOT_REACHABLE");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
  }

  @ExceptionHandler(com.xray.backend.exception.DicomConversionException.class)
  public ResponseEntity<ErrorResponse> handleDicomConversion(com.xray.backend.exception.DicomConversionException ex) {
    logger.error("DICOM conversion error", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "DICOM_CONVERSION_FAILURE");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    logger.warn("Invalid argument", ex);
    ErrorResponse body = new ErrorResponse("error", ex.getMessage(), "INVALID_REQUEST");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    logger.error("Unexpected error", ex);
    ErrorResponse body = new ErrorResponse("error", "Internal server error", "INTERNAL_ERROR");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
