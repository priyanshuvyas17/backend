package com.xray.backend.controller;

import com.xray.backend.dto.StudyCreateRequest;
import com.xray.backend.dto.StudyCreateResponse;
import com.xray.backend.service.StudyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/studies")
@CrossOrigin("*")
public class StudyController {

  private final StudyService studyService;

  public StudyController(StudyService studyService) {
    this.studyService = studyService;
  }

  /**
   * POST /api/studies - Create a new study for a patient.
   */
  @PostMapping
  public ResponseEntity<StudyCreateResponse> createStudy(
      @Valid @RequestBody StudyCreateRequest request) {
    StudyCreateResponse response = studyService.createStudy(request);
    return ResponseEntity.ok(response);
  }

  /**
   * POST /api/studies/{studyUid}/finish - Mark study as completed.
   */
  @PostMapping("/{studyUid}/finish")
  public ResponseEntity<Map<String, String>> finishStudy(@PathVariable String studyUid) {
    studyService.finishStudy(studyUid);
    return ResponseEntity.ok(Map.of("message", "Study completed successfully", "studyUid", studyUid));
  }
}
