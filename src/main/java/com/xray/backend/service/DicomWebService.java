package com.xray.backend.service;

import com.xray.backend.entity.*;
import com.xray.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DicomWebService {

  private static final Logger log = LoggerFactory.getLogger(DicomWebService.class);
  private final ImageStorageService storageService;
  private final PatientRepository patientRepository;
  private final StudyRepository studyRepository;
  private final SeriesRepository seriesRepository;
  private final DicomImageRepository imageRepository;

  public DicomWebService(ImageStorageService storageService,
                         PatientRepository patientRepository,
                         StudyRepository studyRepository,
                         SeriesRepository seriesRepository,
                         DicomImageRepository imageRepository) {
    this.storageService = storageService;
    this.patientRepository = patientRepository;
    this.studyRepository = studyRepository;
    this.seriesRepository = seriesRepository;
    this.imageRepository = imageRepository;
  }

  @Transactional
  public DicomImage store(MultipartFile file) {
    ImageMetadata meta = storageService.store(file);
    Path path = Paths.get(meta.getPath());
    String pn = "Unknown";
    String md = "Unknown";
    LocalDate sdLocal = null;
    String stUid = null;
    String seUid = null;
    String spUid = null;

    try (DicomInputStream dis = new DicomInputStream(path.toFile())) {
      dis.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
      Attributes attrs = dis.readDataset();
      if (attrs != null) {
        pn = attrs.getString(Tag.PatientName, pn);
        md = attrs.getString(Tag.Modality, md);
        String sdv = attrs.getString(Tag.StudyDate, null);
        if (sdv != null && sdv.length() >= 8) {
          sdLocal = LocalDate.parse(sdv.substring(0, 8),
              java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        stUid = attrs.getString(Tag.StudyInstanceUID, null);
        seUid = attrs.getString(Tag.SeriesInstanceUID, null);
        spUid = attrs.getString(Tag.SOPInstanceUID, null);
      }
    } catch (Exception ex) {
      log.warn("Could not read DICOM metadata from {}: {}", path, ex.getMessage());
    }

    final String fpn = pn;
    final String fmd = md;
    final LocalDate fsd = sdLocal;
    final String fStudyUid = stUid;
    final String fSeriesUid = seUid;
    final String fSopUid = spUid;

    Patient patient = patientRepository
        .findByPatientNameAndPatientBirthDate(fpn, null)
        .orElseGet(() -> {
          Patient p = new Patient();
          p.setPatientName(fpn);
          return patientRepository.save(p);
        });

    Study study = studyRepository.findByStudyInstanceUid(fStudyUid)
        .orElseGet(() -> {
          Study s = new Study();
          s.setPatient(patient);
          s.setStudyInstanceUid(fStudyUid != null ? fStudyUid : UUID.randomUUID().toString());
          s.setStudyDate(fsd);
          s.setModality(fmd);
          return studyRepository.save(s);
        });

    Series series = seriesRepository.findBySeriesInstanceUid(fSeriesUid)
        .orElseGet(() -> {
          Series se = new Series();
          se.setStudy(study);
          se.setSeriesInstanceUid(fSeriesUid != null ? fSeriesUid : UUID.randomUUID().toString());
          se.setModality(fmd);
          return seriesRepository.save(se);
        });

    DicomImage image = new DicomImage();
    image.setSeries(series);
    image.setImageUuid(UUID.randomUUID().toString());
    image.setFilePath(meta.getPath());
    image.setPreviewPath(meta.getPreviewPath());
    image.setWidth(meta.getWidth());
    image.setHeight(meta.getHeight());
    image.setFileSize(meta.getSize());
    image.setSopInstanceUid(fSopUid != null ? fSopUid : UUID.randomUUID().toString());
    return imageRepository.save(image);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "studiesSearch", key = "#patientName + '-' + #from + '-' + #to + '-' + #modality", unless = "#result == null")
  public List<Study> searchStudies(String patientName, LocalDate from, LocalDate to, String modality) {
    if (from != null && to != null) {
      return studyRepository.findByStudyDateBetweenOrderByStudyDateDesc(from, to);
    }
    if (modality != null) {
      return studyRepository.findByModalityOrderByStudyDateDesc(modality);
    }
    if (patientName != null) {
      return studyRepository.findAll();
    }
    return studyRepository.findAll();
  }
}
