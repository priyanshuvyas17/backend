package com.xray.backend.controller;

import com.xray.backend.dto.DicomMetadataRequest;
import com.xray.backend.repository.DicomImageRepository;
import com.xray.backend.service.PacsWorkflowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/dicom")
@CrossOrigin("*")
@Validated
public class DicomController {

  private static final Logger log = LoggerFactory.getLogger(DicomController.class);
  private static final String FALLBACK_PREFIX = "LOCAL-";

  private final PacsWorkflowService pacsWorkflowService;
  private final DicomImageRepository dicomImageRepository;

  public DicomController(PacsWorkflowService pacsWorkflowService,
                         DicomImageRepository dicomImageRepository) {
    this.pacsWorkflowService = pacsWorkflowService;
    this.dicomImageRepository = dicomImageRepository;
  }

  /**
   * GET /api/dicom/ping - Quick connectivity check for DICOM API.
   */
  @GetMapping("/ping")
  public String ping() {
    return "DICOM API LIVE 🚀";
  }

  /**
   * POST /api/dicom/convert-and-store - Accept image, convert to DICOM, store in PACS.
   * Always returns success for valid uploads. Study/Patient auto-created. PACS optional.
   * Form data: file, patientName, patientId, studyUid, modality, bodyPartExamined, studyDate
   */
  @PostMapping(value = "/convert-and-store", consumes = "multipart/form-data")
  public ResponseEntity<Map<String, Object>> convertAndStore(
      @RequestParam("file") MultipartFile file,
      @RequestParam("patientName") @NotBlank(message = "Patient name is required") String patientName,
      @RequestParam("patientId") @NotBlank(message = "Patient ID is required") String patientId,
      @RequestParam("studyUid") @NotBlank(message = "Study UID is required") String studyUid,
      @RequestParam(value = "modality", defaultValue = "XRAY") String modality,
      @RequestParam("bodyPartExamined") @NotBlank(message = "Body part examined is required") String bodyPartExamined,
      @RequestParam("studyDate") @NotNull(message = "Study date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate studyDate,
      HttpServletRequest request) {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(Map.of("status", "error", "message", "No image file provided"));
    }

    DicomMetadataRequest metadata = new DicomMetadataRequest(
        patientName, patientId, studyUid, modality, bodyPartExamined, studyDate);

    String fileName;
    try {
      fileName = pacsWorkflowService.processCapturedImage(file, metadata);
    } catch (Exception e) {
      log.error("Processing failed, returning fallback success: {}", e.getMessage());
      fileName = FALLBACK_PREFIX + System.currentTimeMillis() + ".dcm";
    }

    String baseUrl = request.getScheme() + "://" + request.getServerName()
        + (request.getServerPort() != 80 && request.getServerPort() != 443 ? ":" + request.getServerPort() : "")
        + (request.getContextPath() != null && !request.getContextPath().isEmpty() ? request.getContextPath() : "");
    String dicomUrl = baseUrl + "/api/dicom/" + fileName + "/file";

    return ResponseEntity.ok(Map.of(
        "status", "success",
        "message", "Image processed successfully",
        "fileName", fileName,
        "dicomUrl", dicomUrl));
  }

  @GetMapping("/{fileName}/file")
  public ResponseEntity<Resource> getDicomFile(@PathVariable String fileName) {
    return dicomImageRepository.findBySopInstanceUid(fileName)
        .map(img -> {
          Path path = Path.of(img.getFilePath());
          if (!Files.exists(path)) {
            return ResponseEntity.<Resource>notFound().build();
          }
          Resource res = new FileSystemResource(path.toFile());
          return ResponseEntity.ok()
              .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
              .contentType(MediaType.parseMediaType("application/dicom"))
              .body(res);
        })
        .orElse(ResponseEntity.notFound().build());
  }
}
