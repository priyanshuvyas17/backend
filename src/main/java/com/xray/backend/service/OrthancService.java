package com.xray.backend.service;

import com.xray.backend.config.OrthancProperties;
import com.xray.backend.exception.PacsConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with Orthanc PACS server.
 * Stores DICOM instances and fetches patient/study/series data.
 */
@Service
public class OrthancService {

  private static final Logger log = LoggerFactory.getLogger(OrthancService.class);

  private final OrthancProperties orthanc;
  private final RestTemplate restTemplate;

  public OrthancService(OrthancProperties orthanc) {
    this.orthanc = orthanc;
    org.springframework.http.client.SimpleClientHttpRequestFactory factory =
        new org.springframework.http.client.SimpleClientHttpRequestFactory();
    factory.setConnectTimeout((int) (orthanc.getTimeoutSeconds() * 1000L));
    factory.setReadTimeout((int) (orthanc.getTimeoutSeconds() * 1000L));
    this.restTemplate = new RestTemplate(factory);
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    if (orthanc.getUsername() != null && !orthanc.getUsername().isEmpty()) {
      String auth = orthanc.getUsername() + ":" + (orthanc.getPassword() != null ? orthanc.getPassword() : "");
      String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
      headers.set("Authorization", "Basic " + encoded);
    }
    return headers;
  }

  /**
   * Store a DICOM file in Orthanc PACS.
   * POST http://localhost:8042/instances
   *
   * @param dicomPath Path to the DICOM file
   * @return Orthanc instance ID
   */
  public String storeInstance(Path dicomPath) {
    if (!Files.exists(dicomPath)) {
      throw new PacsConnectionException("DICOM file not found: " + dicomPath);
    }
    String url = orthanc.getUrl().replaceAll("/$", "") + "/instances";
    try {
      byte[] body = Files.readAllBytes(dicomPath);
      HttpHeaders headers = authHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        String id = (String) response.getBody().get("ID");
        log.info("DICOM stored in Orthanc, instance ID: {}", id);
        return id;
      }
      throw new PacsConnectionException("Orthanc returned: " + response.getStatusCode());
    } catch (ResourceAccessException e) {
      throw new PacsConnectionException("PACS not reachable at " + url + ": " + e.getMessage(), e);
    } catch (Exception e) {
      throw new PacsConnectionException("Failed to store DICOM in PACS: " + e.getMessage(), e);
    }
  }

  /**
   * Get list of patients from Orthanc.
   */
  @SuppressWarnings("unchecked")
  public List<String> getPatients() {
    String url = orthanc.getUrl().replaceAll("/$", "") + "/patients";
    try {
      HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
      ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
      return response.getBody() != null ? response.getBody() : List.of();
    } catch (ResourceAccessException e) {
      throw new PacsConnectionException("PACS not reachable: " + e.getMessage(), e);
    }
  }

  /**
   * Get list of studies from Orthanc.
   */
  @SuppressWarnings("unchecked")
  public List<String> getStudies() {
    String url = orthanc.getUrl().replaceAll("/$", "") + "/studies";
    try {
      HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
      ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
      return response.getBody() != null ? response.getBody() : List.of();
    } catch (ResourceAccessException e) {
      throw new PacsConnectionException("PACS not reachable: " + e.getMessage(), e);
    }
  }

  /**
   * Get list of series from Orthanc.
   */
  @SuppressWarnings("unchecked")
  public List<String> getSeriesList() {
    String url = orthanc.getUrl().replaceAll("/$", "") + "/series";
    try {
      HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
      ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
      return response.getBody() != null ? response.getBody() : List.of();
    } catch (ResourceAccessException e) {
      throw new PacsConnectionException("PACS not reachable: " + e.getMessage(), e);
    }
  }

  /**
   * Get patient details from Orthanc.
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> getPatient(String patientId) {
    String url = orthanc.getUrl().replaceAll("/$", "") + "/patients/" + patientId;
    try {
      HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
      return response.getBody() != null ? response.getBody() : Map.of();
    } catch (ResourceAccessException e) {
      throw new PacsConnectionException("PACS not reachable: " + e.getMessage(), e);
    }
  }

  /**
   * Get series details from Orthanc.
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> getSeries(String seriesId) {
    String url = orthanc.getUrl().replaceAll("/$", "") + "/series/" + seriesId;
    try {
      HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
      return response.getBody() != null ? response.getBody() : Map.of();
    } catch (ResourceAccessException e) {
      throw new PacsConnectionException("PACS not reachable: " + e.getMessage(), e);
    }
  }

  /**
   * Get study details from Orthanc.
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> getStudy(String studyId) {
    String url = orthanc.getUrl().replaceAll("/$", "") + "/studies/" + studyId;
    try {
      HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
      return response.getBody() != null ? response.getBody() : Map.of();
    } catch (ResourceAccessException e) {
      throw new PacsConnectionException("PACS not reachable: " + e.getMessage(), e);
    }
  }

  /**
   * Check if Orthanc is reachable.
   */
  public boolean isReachable() {
    try {
      String url = orthanc.getUrl().replaceAll("/$", "") + "/system";
      HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
      restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
      return true;
    } catch (Exception e) {
      log.warn("Orthanc not reachable: {}", e.getMessage());
      return false;
    }
  }
}
