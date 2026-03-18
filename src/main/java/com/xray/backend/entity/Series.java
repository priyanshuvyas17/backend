package com.xray.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "series",
       uniqueConstraints = @UniqueConstraint(name = "uk_series_uid", columnNames = "series_instance_uid"),
       indexes = {
         @Index(name = "idx_series_study", columnList = "study_id"),
         @Index(name = "idx_series_study_number", columnList = "study_id,series_number"),
         @Index(name = "idx_series_modality", columnList = "modality")
       })
public class Series {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "series_id")
  private Long seriesId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "study_id", nullable = false)
  private Study study;

  @Column(name = "series_instance_uid", nullable = false, length = 64)
  private String seriesInstanceUid;

  @Column(name = "series_number")
  private Integer seriesNumber;

  @Column(name = "body_part_examined", length = 64)
  private String bodyPartExamined;

  @Column(name = "modality", length = 16)
  private String modality;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "series", cascade = CascadeType.REMOVE, orphanRemoval = true)
  private List<DicomImage> images = new ArrayList<>();

  public Long getSeriesId() {
    return seriesId;
  }

  public Study getStudy() {
    return study;
  }

  public void setStudy(Study study) {
    this.study = study;
  }

  public String getSeriesInstanceUid() {
    return seriesInstanceUid;
  }

  public void setSeriesInstanceUid(String seriesInstanceUid) {
    this.seriesInstanceUid = seriesInstanceUid;
  }

  public Integer getSeriesNumber() {
    return seriesNumber;
  }

  public void setSeriesNumber(Integer seriesNumber) {
    this.seriesNumber = seriesNumber;
  }

  public String getBodyPartExamined() {
    return bodyPartExamined;
  }

  public void setBodyPartExamined(String bodyPartExamined) {
    this.bodyPartExamined = bodyPartExamined;
  }

  public String getModality() {
    return modality;
  }

  public void setModality(String modality) {
    this.modality = modality;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public List<DicomImage> getImages() {
    return images;
  }
}
