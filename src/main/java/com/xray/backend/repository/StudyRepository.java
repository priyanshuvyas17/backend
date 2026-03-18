package com.xray.backend.repository;

import com.xray.backend.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long> {
  Optional<Study> findByStudyInstanceUid(String uid);
  List<Study> findByPatient_PatientIdOrderByStudyDateDesc(Long patientId);
  List<Study> findByStudyDateBetweenOrderByStudyDateDesc(LocalDate from, LocalDate to);
  List<Study> findByModalityOrderByStudyDateDesc(String modality);
}
