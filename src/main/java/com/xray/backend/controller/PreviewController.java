package com.xray.backend.controller;

import com.xray.backend.model.ScanInfoResponse;
import com.xray.backend.service.DicomPreviewService;
import com.xray.backend.service.ScanInfoService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class PreviewController {

  private final DicomPreviewService dicomPreviewService;
  private final ScanInfoService scanInfoService;

  public PreviewController(DicomPreviewService dicomPreviewService, ScanInfoService scanInfoService) {
    this.dicomPreviewService = dicomPreviewService;
    this.scanInfoService = scanInfoService;
  }

  /**
   * GET /api/preview/{fileName}
   * - JPG/PNG -> returns original bytes directly
   * - DCM/DCN -> returns cached/generated PNG preview
   */
  @GetMapping("/preview/{fileName}")
  public ResponseEntity<Resource> preview(@PathVariable String fileName) {
    DicomPreviewService.PreviewResult result = dicomPreviewService.getOrCreatePreview(fileName);

    MediaType mediaType = switch (result.contentType().toLowerCase()) {
      case "image/png" -> MediaType.IMAGE_PNG;
      case "image/jpeg" -> MediaType.IMAGE_JPEG;
      default -> MediaType.APPLICATION_OCTET_STREAM;
    };

    Resource resource = new FileSystemResource(result.path());
    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + result.path().getFileName() + "\"")
        .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
        .body(resource);
  }

  /**
   * GET /api/scan-info/{fileName}
   * Returns key DICOM tags as JSON.
   */
  @GetMapping("/scan-info/{fileName}")
  public ResponseEntity<ScanInfoResponse> scanInfo(@PathVariable String fileName) {
    return ResponseEntity.ok(scanInfoService.getScanInfo(fileName));
  }
}

