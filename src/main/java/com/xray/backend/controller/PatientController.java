package com.xray.backend.controller;

import com.xray.backend.dto.PatientRegistrationRequest;
import com.xray.backend.dto.PatientRegistrationResponse;
import com.xray.backend.dto.PatientRecordDto;
import com.xray.backend.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin("*")
public class PatientController {

  private final PatientService patientService;

  public PatientController(PatientService patientService) {
    this.patientService = patientService;
  }

  /**
   * POST /api/patients - Register a new patient.
   */
  @PostMapping
  public ResponseEntity<PatientRegistrationResponse> registerPatient(
      @Valid @RequestBody PatientRegistrationRequest request) {
    PatientRegistrationResponse response = patientService.registerPatient(request);
    return ResponseEntity.ok(response);
  }

  /**
   * GET /api/patients - List all patients.
   */
  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> listPatients() {
    var patients = patientService.getAllPatients().stream()
        .map(p -> Map.<String, Object>of(
            "patientUid", p.getPatientUid(),
            "patientId", p.getPatientId(),
            "patientName", p.getPatientName(),
            "age", p.getAge() != null ? p.getAge() : 0,
            "gender", p.getPatientSex() != null ? p.getPatientSex() : "",
            "bodyPartExamined", p.getBodyPartExamined() != null ? p.getBodyPartExamined() : "",
            "studyType", p.getStudyType() != null ? p.getStudyType() : ""
        ))
        .toList();
    return ResponseEntity.ok(patients);
  }

  /**
   * GET /api/patients/{patientUid}/records - Get patient records (studies with images).
   */
  @GetMapping("/{patientUid}/records")
  public ResponseEntity<?> getPatientRecords(@PathVariable String patientUid) {
    List<PatientRecordDto> records = patientService.getPatientRecords(patientUid);
    if (records.isEmpty()) {
      return ResponseEntity.ok(Map.of("message", "No studies available.", "records", List.of()));
    }
    return ResponseEntity.ok(Map.of("records", records));
  }

  /**
   * GET /api/patients/records - Get all patient records.
   */
  @GetMapping("/records")
  public ResponseEntity<Map<String, Object>> getAllRecords() {
    List<PatientRecordDto> records = patientService.getAllPatientRecords();
    if (records.isEmpty()) {
      return ResponseEntity.ok(Map.of("message", "No studies available.", "records", List.of()));
    }
    return ResponseEntity.ok(Map.of("records", records));
  }
}
