package com.xray.backend.repository;

import com.xray.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
  Optional<Patient> findByPatientUid(String patientUid);
  Optional<Patient> findByPatientNameAndPatientBirthDate(String name, java.time.LocalDate birthDate);
}
