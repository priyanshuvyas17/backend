package com.xray.backend.repository;

import com.xray.backend.entity.ImageMetadata;
import com.xray.backend.exception.StoredFileNotFoundException;
import com.xray.backend.utils.FileTypeUtils;
import org.springframework.stereotype.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Repository
public class StoredFileRepository {

  private final ImageMetadataRepository imageMetadataRepository;

  public StoredFileRepository(ImageMetadataRepository imageMetadataRepository) {
    this.imageMetadataRepository = imageMetadataRepository;
  }

  public Path resolveUploadedPathOrThrow(String fileName) {
    FileTypeUtils.validateSafeFileName(fileName);

    ImageMetadata meta = imageMetadataRepository.findByFileName(fileName)
        .orElseThrow(() -> new StoredFileNotFoundException("File not found: " + fileName));

    Path p = Paths.get(meta.getPath()).toAbsolutePath().normalize();
    if (!Files.exists(p)) {
      throw new StoredFileNotFoundException("File not found on disk: " + fileName);
    }
    return p;
  }
}

