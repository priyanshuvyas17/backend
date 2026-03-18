package com.xray.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients",
       indexes = {
         @Index(name = "idx_patient_name", columnList = "patient_name"),
         @Index(name = "idx_patient_uid", columnList = "patient_uid"),
         @Index(name = "idx_patient_name_dob", columnList = "patient_name,patient_birth_date")
       })
public class Patient {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "patient_id")
  private Long patientId;

  @Column(name = "patient_uid", nullable = false, unique = true, length = 64)
  private String patientUid;

  @Column(name = "patient_id_external", length = 64)
  private String patientIdExternal;

  @Column(name = "patient_name", nullable = false, length = 255)
  private String patientName;

  @Column(name = "patient_birth_date")
  private LocalDate patientBirthDate;

  @Column(name = "patient_sex", length = 16)
  private String patientSex;

  @Column(name = "age")
  private Integer age;

  @Column(name = "body_part_examined", length = 64)
  private String bodyPartExamined;

  @Column(name = "study_type", length = 64)
  private String studyType;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "patient", cascade = CascadeType.REMOVE, orphanRemoval = true)
  private List<Study> studies = new ArrayList<>();

  public Long getPatientId() {
    return patientId;
  }

  public String getPatientUid() {
    return patientUid;
  }

  public void setPatientUid(String patientUid) {
    this.patientUid = patientUid;
  }

  public String getPatientIdExternal() {
    return patientIdExternal;
  }

  public void setPatientIdExternal(String patientIdExternal) {
    this.patientIdExternal = patientIdExternal;
  }

  public String getPatientName() {
    return patientName;
  }

  public void setPatientName(String patientName) {
    this.patientName = patientName;
  }

  public LocalDate getPatientBirthDate() {
    return patientBirthDate;
  }

  public void setPatientBirthDate(LocalDate patientBirthDate) {
    this.patientBirthDate = patientBirthDate;
  }

  public String getPatientSex() {
    return patientSex;
  }

  public void setPatientSex(String patientSex) {
    this.patientSex = patientSex;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public String getBodyPartExamined() {
    return bodyPartExamined;
  }

  public void setBodyPartExamined(String bodyPartExamined) {
    this.bodyPartExamined = bodyPartExamined;
  }

  public String getStudyType() {
    return studyType;
  }

  public void setStudyType(String studyType) {
    this.studyType = studyType;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public List<Study> getStudies() {
    return studies;
  }
}
