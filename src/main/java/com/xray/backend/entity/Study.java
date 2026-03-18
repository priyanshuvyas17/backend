package com.xray.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "studies",
       uniqueConstraints = @UniqueConstraint(name = "uk_study_uid", columnNames = "study_instance_uid"),
       indexes = {
         @Index(name = "idx_study_patient_date", columnList = "patient_id,study_date"),
         @Index(name = "idx_study_modality", columnList = "modality")
       })
public class Study {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "study_id")
  private Long studyId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @Column(name = "study_instance_uid", nullable = false, length = 64)
  private String studyInstanceUid;

  @Column(name = "study_date")
  private LocalDate studyDate;

  @Column(name = "modality", length = 16)
  private String modality;

  @Column(name = "referring_physician", length = 255)
  private String referringPhysician;

  @Column(name = "study_status", length = 32)
  private String studyStatus = "IN_PROGRESS";

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "study", cascade = CascadeType.REMOVE, orphanRemoval = true)
  private List<Series> seriesList = new ArrayList<>();

  public Long getStudyId() {
    return studyId;
  }

  public Patient getPatient() {
    return patient;
  }

  public void setPatient(Patient patient) {
    this.patient = patient;
  }

  public String getStudyInstanceUid() {
    return studyInstanceUid;
  }

  public void setStudyInstanceUid(String studyInstanceUid) {
    this.studyInstanceUid = studyInstanceUid;
  }

  public LocalDate getStudyDate() {
    return studyDate;
  }

  public void setStudyDate(LocalDate studyDate) {
    this.studyDate = studyDate;
  }

  public String getModality() {
    return modality;
  }

  public void setModality(String modality) {
    this.modality = modality;
  }

  public String getReferringPhysician() {
    return referringPhysician;
  }

  public void setReferringPhysician(String referringPhysician) {
    this.referringPhysician = referringPhysician;
  }

  public String getStudyStatus() {
    return studyStatus;
  }

  public void setStudyStatus(String studyStatus) {
    this.studyStatus = studyStatus;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public List<Series> getSeriesList() {
    return seriesList;
  }
}
