package com.xray.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "images",
       uniqueConstraints = {
         @UniqueConstraint(name = "uk_image_uuid", columnNames = "image_uuid"),
         @UniqueConstraint(name = "uk_sop_uid", columnNames = "sop_instance_uid")
       },
       indexes = {
         @Index(name = "idx_image_series", columnList = "series_id"),
         @Index(name = "idx_image_series_created", columnList = "series_id,created_at")
       })
public class DicomImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "image_id")
  private Long imageId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "series_id", nullable = false)
  private Series series;

  @Column(name = "image_uuid", nullable = false, length = 36)
  private String imageUuid;

  @Column(name = "file_path", nullable = false, length = 1024)
  private String filePath;

  @Column(name = "preview_path", length = 1024)
  private String previewPath;

  @Column(name = "width")
  private Integer width;

  @Column(name = "height")
  private Integer height;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "sop_instance_uid", nullable = false, length = 64)
  private String sopInstanceUid;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public Long getImageId() {
    return imageId;
  }

  public Series getSeries() {
    return series;
  }

  public void setSeries(Series series) {
    this.series = series;
  }

  public String getImageUuid() {
    return imageUuid;
  }

  public void setImageUuid(String imageUuid) {
    this.imageUuid = imageUuid;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
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

  public Long getFileSize() {
    return fileSize;
  }

  public void setFileSize(Long fileSize) {
    this.fileSize = fileSize;
  }

  public String getSopInstanceUid() {
    return sopInstanceUid;
  }

  public void setSopInstanceUid(String sopInstanceUid) {
    this.sopInstanceUid = sopInstanceUid;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
