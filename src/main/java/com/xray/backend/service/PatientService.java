package com.xray.backend.service;

import com.xray.backend.dto.PatientRegistrationRequest;
import com.xray.backend.dto.PatientRegistrationResponse;
import com.xray.backend.dto.PatientRecordDto;
import com.xray.backend.entity.Patient;
import com.xray.backend.entity.Study;
import com.xray.backend.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PatientService {

  private static final Logger log = LoggerFactory.getLogger(PatientService.class);

  private final PatientRepository patientRepository;

  public PatientService(PatientRepository patientRepository) {
    this.patientRepository = patientRepository;
  }

  @Transactional
  public PatientRegistrationResponse registerPatient(PatientRegistrationRequest request) {
    String patientUid = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    Patient patient = new Patient();
    patient.setPatientUid(patientUid);
    patient.setPatientIdExternal(request.patientId());
    patient.setPatientName(request.patientName());
    patient.setAge(request.age());
    patient.setPatientSex(request.gender());
    patient.setBodyPartExamined(request.bodyPartExamined());
    patient.setStudyType(request.studyType());
    patient.setPatientBirthDate(null); // Optional - can derive from age
    patientRepository.save(patient);
    log.info("Registered patient: {} (UID: {})", request.patientName(), patientUid);
    return new PatientRegistrationResponse(
        patientUid,
        patient.getPatientId(),
        patient.getPatientName(),
        patient.getPatientIdExternal(),
        "Patient registered successfully"
    );
  }

  public Optional<Patient> findByPatientUid(String patientUid) {
    return patientRepository.findByPatientUid(patientUid);
  }

  public List<Patient> getAllPatients() {
    return patientRepository.findAll();
  }

  public List<PatientRecordDto> getPatientRecords(String patientUid) {
    return patientRepository.findByPatientUid(patientUid)
        .map(this::toRecordDtos)
        .orElse(List.of());
  }

  public List<PatientRecordDto> getAllPatientRecords() {
    return patientRepository.findAll().stream()
        .flatMap(p -> toRecordDtos(p).stream())
        .collect(Collectors.toList());
  }

  private List<PatientRecordDto> toRecordDtos(Patient patient) {
    return patient.getStudies().stream()
        .filter(s -> "COMPLETED".equals(s.getStudyStatus()))
        .map(study -> toRecordDto(patient, study))
        .collect(Collectors.toList());
  }

  private PatientRecordDto toRecordDto(Patient patient, Study study) {
    int imageCount = study.getSeriesList().stream()
        .mapToInt(s -> s.getImages().size())
        .sum();
    var images = study.getSeriesList().stream()
        .flatMap(s -> s.getImages().stream())
        .map(img -> new PatientRecordDto.ImageRecordDto(
            img.getFilePath() != null ? Paths.get(img.getFilePath()).getFileName().toString() : "unknown",
            img.getCreatedAt(),
            img.getFileSize()
        ))
        .toList();
    return new PatientRecordDto(
        patient.getPatientName(),
        patient.getPatientIdExternal() != null ? patient.getPatientIdExternal() : patient.getPatientUid(),
        study.getStudyDate(),
        study.getModality() != null ? study.getModality() : "XRAY",
        patient.getBodyPartExamined() != null ? patient.getBodyPartExamined() : "",
        imageCount,
        images
    );
  }
}
