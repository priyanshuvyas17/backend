package com.xray.backend.repository;

import com.xray.backend.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SeriesRepository extends JpaRepository<Series, Long> {
  Optional<Series> findBySeriesInstanceUid(String uid);
  List<Series> findByStudy_StudyInstanceUidOrderBySeriesNumberAsc(String studyUid);
}
