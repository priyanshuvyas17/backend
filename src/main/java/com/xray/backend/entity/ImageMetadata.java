package com.xray.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_metadata")
public class ImageMetadata {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "original_file_name", nullable = false, length = 255)
  private String originalFileName;

  @Column(name = "content_type", length = 128)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private Long size;

  @Column(name = "file_path", nullable = false, length = 1024)
  private String path;

  @Column(name = "upload_time", nullable = false)
  private LocalDateTime uploadTime;

  @Column(name = "patient_name", length = 255)
  private String patientName;

  @Column(name = "modality", length = 64)
  private String modality;

  @Column(name = "study_date", length = 16)
  private String studyDate;

  @Column(name = "preview_path", length = 1024)
  private String previewPath;

  @Column(name = "image_width")
  private Integer width;

  @Column(name = "image_height")
  private Integer height;

  public Long getId() {
    return id;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getOriginalFileName() {
    return originalFileName;
  }

  public void setOriginalFileName(String originalFileName) {
    this.originalFileName = originalFileName;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public LocalDateTime getUploadTime() {
    return uploadTime;
  }

  public void setUploadTime(LocalDateTime uploadTime) {
    this.uploadTime = uploadTime;
  }

  public String getPatientName() {
    return patientName;
  }

  public void setPatientName(String patientName) {
    this.patientName = patientName;
  }

  public String getModality() {
    return modality;
  }

  public void setModality(String modality) {
    this.modality = modality;
  }

  public String getStudyDate() {
    return studyDate;
  }

  public void setStudyDate(String studyDate) {
    this.studyDate = studyDate;
  }

  public String getPreviewPath() {
    return previewPath;
  }

  public void setPreviewPath(String previewPath) {
    this.previewPath = previewPath;
  }

  public Integer getWidth() {
    return width;
  }

  public void setWidth(Integer width) {
    this.width = width;
  }

  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }
}
