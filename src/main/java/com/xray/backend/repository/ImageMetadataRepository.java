package com.xray.backend.repository;

import com.xray.backend.entity.ImageMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, Long> {
  Optional<ImageMetadata> findByFileName(String fileName);
}

