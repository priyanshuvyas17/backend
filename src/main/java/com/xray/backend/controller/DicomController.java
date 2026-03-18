package com.xray.backend.controller;

import com.xray.backend.dto.DicomMetadataRequest;
import com.xray.backend.service.PacsWorkflowService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/dicom")
@CrossOrigin("*")
@Validated
public class DicomController {

  private final PacsWorkflowService pacsWorkflowService;

  public DicomController(PacsWorkflowService pacsWorkflowService) {
    this.pacsWorkflowService = pacsWorkflowService;
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
      @RequestParam("studyDate") @NotNull(message = "Study date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate studyDate) {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(Map.of("status", "error", "message", "No image file provided"));
    }
    DicomMetadataRequest metadata = new DicomMetadataRequest(
        patientName, patientId, studyUid, modality, bodyPartExamined, studyDate);
    String instanceId = pacsWorkflowService.processCapturedImage(file, metadata);
    return ResponseEntity.ok(Map.of(
        "status", "success",
        "message", "Image converted to DICOM and stored in PACS",
        "instanceId", instanceId));
  }
}
