package com.xray.backend.service;

import com.xray.backend.dto.DicomMetadataRequest;
import com.xray.backend.dto.StudyCreateRequest;
import com.xray.backend.dto.StudyCreateResponse;
import com.xray.backend.entity.Patient;
import com.xray.backend.entity.Study;
import com.xray.backend.repository.StudyRepository;
import org.dcm4che3.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class StudyService {

  private static final Logger log = LoggerFactory.getLogger(StudyService.class);
  private static final String MODALITY_XRAY = "XRAY";

  private final StudyRepository studyRepository;
  private final PatientService patientService;

  public StudyService(StudyRepository studyRepository, PatientService patientService) {
    this.studyRepository = studyRepository;
    this.patientService = patientService;
  }

  @Transactional
  public StudyCreateResponse createStudy(StudyCreateRequest request) {
    Patient patient = patientService.findByPatientUid(request.patientUid())
        .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + request.patientUid()));

    String studyUid = UIDUtils.createUID();
    Study study = new Study();
    study.setPatient(patient);
    study.setStudyInstanceUid(studyUid);
    study.setStudyDate(request.studyDate());
    study.setModality(MODALITY_XRAY);
    study.setStudyStatus("IN_PROGRESS");
    studyRepository.save(study);
    log.info("Created study: {} for patient {}", studyUid, request.patientUid());
    return new StudyCreateResponse(
        studyUid,
        study.getStudyId(),
        request.patientUid(),
        MODALITY_XRAY,
        request.studyDate().toString(),
        "Study created successfully"
    );
  }

  public Optional<Study> findByStudyUid(String studyUid) {
    return studyRepository.findByStudyInstanceUid(studyUid);
  }

  /**
   * Get existing study or create one for dev/testing when convert-and-store is used.
   */
  @Transactional
  public Study getOrCreateStudyForDicom(DicomMetadataRequest metadata) {
    return studyRepository.findByStudyInstanceUid(metadata.studyUid())
        .orElseGet(() -> {
          Patient patient = patientService.findOrCreateByPatientIdAndName(
              metadata.patientId(), metadata.patientName());
          Study study = new Study();
          study.setPatient(patient);
          study.setStudyInstanceUid(metadata.studyUid());
          study.setStudyDate(metadata.studyDate());
          study.setModality(metadata.modality());
          study.setStudyStatus("IN_PROGRESS");
          studyRepository.save(study);
          log.info("Auto-created study {} for patient {} (dev/testing)", metadata.studyUid(), metadata.patientId());
          return study;
        });
  }

  @Transactional
  public void finishStudy(String studyUid) {
    Study study = studyRepository.findByStudyInstanceUid(studyUid)
        .orElseThrow(() -> new IllegalArgumentException("Study not found: " + studyUid));
    study.setStudyStatus("COMPLETED");
    studyRepository.save(study);
    log.info("Study completed: {}", studyUid);
  }
}
