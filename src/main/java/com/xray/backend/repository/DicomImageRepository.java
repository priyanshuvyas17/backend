package com.xray.backend.repository;

import com.xray.backend.entity.DicomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DicomImageRepository extends JpaRepository<DicomImage, Long> {
  Optional<DicomImage> findBySopInstanceUid(String uid);
  List<DicomImage> findBySeries_SeriesInstanceUidOrderByCreatedAtDesc(String seriesUid);
}
