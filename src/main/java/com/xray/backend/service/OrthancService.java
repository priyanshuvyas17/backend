package com.xray.backend.service;

import com.xray.backend.config.OrthancProperties;
import com.xray.backend.dto.pacs.OrthancHealthResult;
import com.xray.backend.exception.PacsConnectionException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * Production REST client for Orthanc PACS ({@code /patients}, {@code /studies}, {@code /instances}, etc.)
 * using HTTP Basic authentication, transient retries, and streaming downloads for WADO-style access.
 */
@Service
public class OrthancService {

  private static final Logger log = LoggerFactory.getLogger(OrthancService.class);

  private static final Pattern ORTHANC_INSTANCE_ID = Pattern.compile("^[a-fA-F0-9-]{8,64}$");

  private final OrthancProperties orthanc;
  private final RestTemplate restTemplate;

  public OrthancService(OrthancProperties orthanc) {
    this.orthanc = orthanc;
    var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    factory.setConnectTimeout((int) (orthanc.getTimeoutSeconds() * 1000L));
    factory.setReadTimeout((int) (orthanc.getTimeoutSeconds() * 1000L));
    this.restTemplate = new RestTemplate(factory);
  }

  @PostConstruct
  void logConfiguration() {
    if (!orthanc.isConfigured()) {
      log.warn(
          "Orthanc is not configured (orthanc.enabled=false or orthanc.url is blank). "
              + "Set orthanc.url or ORTHANC_URL. On Render, localhost will NOT reach a laptop.");
      return;
    }
    log.info(
        "Orthanc client: target={}, auth={}, timeout={}s, retries={}",
        orthanc.normalizedBaseUrl(),
        (orthanc.getUsername() != null && !orthanc.getUsername().isEmpty()) ? "Basic" : "none",
        orthanc.getTimeoutSeconds(),
        orthanc.getMaxRetries());
  }

  /** Exposed for routing (local file vs Orthanc). */
  public boolean isConfigured() {
    return orthanc.isConfigured();
  }

  private String baseUrl() {
    return orthanc.normalizedBaseUrl();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    applyBasicAuth(headers);
    return headers;
  }

  /** For raw DICOM bytes from Orthanc ({@code /instances/{id}/file}). */
  private HttpHeaders authHeadersForBinary() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
    applyBasicAuth(headers);
    return headers;
  }

  private void applyBasicAuth(HttpHeaders headers) {
    if (orthanc.getUsername() != null && !orthanc.getUsername().isEmpty()) {
      String pw = orthanc.getPassword() != null ? orthanc.getPassword() : "";
      String auth = orthanc.getUsername() + ":" + pw;
      String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
      headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
    }
  }

  private void requireConfigured() {
    if (!orthanc.isConfigured()) {
      throw new PacsConnectionException(
          "Orthanc is not configured. Set orthanc.url (or ORTHANC_URL). "
              + "Cloud deployments cannot use localhost to reach Orthanc on another machine.");
    }
  }

  private static void sleepMs(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Retries only on {@link ResourceAccessException} (timeouts, connection resets).
   */
  private <T> T executeWithRetry(String operation, String fullUrl, Callable<T> callable) {
    int maxAttempts = orthanc.getMaxRetries() + 1;
    long baseDelay = orthanc.getRetryDelayMs();
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return callable.call();
      } catch (ResourceAccessException e) {
        log.warn("Orthanc {} transient failure (attempt {}/{}): {}", operation, attempt, maxAttempts, e.getMessage());
        if (attempt >= maxAttempts) {
          throw mapResourceAccess(fullUrl, e);
        }
        sleepMs(baseDelay * attempt);
      } catch (HttpClientErrorException e) {
        throw mapHttpClient(operation, fullUrl, e);
      } catch (HttpServerErrorException e) {
        throw mapHttpServer(operation, e);
      } catch (PacsConnectionException e) {
        throw e;
      } catch (Exception e) {
        throw new PacsConnectionException("Orthanc " + operation + ": " + e.getMessage(), e);
      }
    }
    throw new IllegalStateException("Unreachable retry loop");
  }

  private PacsConnectionException mapResourceAccess(String url, ResourceAccessException e) {
    Throwable root = e.getMostSpecificCause();
    String detail = root.getMessage() != null ? root.getMessage() : e.getMessage();
    log.warn("Orthanc request failed [{}]: {}", url, detail);
    if (detail != null) {
      String lower = detail.toLowerCase();
      if (lower.contains("connection refused") || root instanceof java.net.ConnectException) {
        return new PacsConnectionException(
            "Connection refused — nothing is listening at Orthanc. Start Orthanc or fix orthanc.url ("
                + orthanc.normalizedBaseUrl()
                + ").",
            e);
      }
      if (root instanceof java.net.SocketTimeoutException
          || lower.contains("timed out")
          || lower.contains("read timed out")) {
        return new PacsConnectionException(
            "Orthanc request timed out (exceeded " + orthanc.getTimeoutSeconds() + "s).", e);
      }
    }
    return new PacsConnectionException("Cannot reach Orthanc at " + orthanc.normalizedBaseUrl() + ": " + detail, e);
  }

  private PacsConnectionException mapHttpClient(String operation, String url, HttpClientErrorException e) {
    int code = e.getStatusCode().value();
    log.warn("Orthanc {} {} returned HTTP {}", operation, orthanc.normalizedBaseUrl(), code);
    if (code == HttpStatus.UNAUTHORIZED.value()) {
      return new PacsConnectionException(
          "Orthanc returned 401 Unauthorized — check orthanc.username and orthanc.password (default: orthanc/orthanc).",
          e);
    }
    if (code == HttpStatus.NOT_FOUND.value()) {
      return new PacsConnectionException("Orthanc: resource not found (" + operation + ").", e);
    }
    return new PacsConnectionException(
        "Orthanc HTTP " + code + " on " + operation + ": " + e.getStatusText(), e);
  }

  private PacsConnectionException mapHttpServer(String operation, HttpServerErrorException e) {
    log.error("Orthanc {} server error: {}", operation, e.getStatusCode());
    return new PacsConnectionException(
        "Orthanc server error " + e.getStatusCode().value() + ": " + e.getStatusText(), e);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getOrthancMap(String path) {
    requireConfigured();
    String url = baseUrl() + path;
    log.debug("Orthanc GET {}", url);
    return executeWithRetry(
        "GET " + path,
        url,
        () -> {
          HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
          ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
          if (response.getBody() == null) {
            return Map.of();
          }
          return (Map<String, Object>) (Map<?, ?>) response.getBody();
        });
  }

  private List<String> getIdList(String path) {
    requireConfigured();
    String url = baseUrl() + path;
    log.debug("Orthanc GET {}", url);
    return executeWithRetry(
        "GET " + path,
        url,
        () -> {
          HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
          ResponseEntity<List<String>> response =
              restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
          return response.getBody() != null ? response.getBody() : List.of();
        });
  }

  /**
   * POST /instances — store DICOM file bytes.
   */
  @SuppressWarnings("unchecked")
  public String storeInstance(Path dicomPath) {
    requireConfigured();
    if (!Files.exists(dicomPath)) {
      throw new PacsConnectionException("DICOM file not found: " + dicomPath);
    }
    String url = baseUrl() + "/instances";
    log.info("Orthanc POST {} (store instance)", url);
    return executeWithRetry(
        "POST /instances",
        url,
        () -> {
          byte[] body = Files.readAllBytes(dicomPath);
          HttpHeaders headers = authHeaders();
          headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
          HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);
          ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
          if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String id = (String) response.getBody().get("ID");
            log.info("Orthanc stored instance id={}", id);
            return id;
          }
          throw new PacsConnectionException("Orthanc returned: " + response.getStatusCode());
        });
  }

  /** GET /patients — list of patient IDs. */
  public List<String> getPatients() {
    return getIdList("/patients");
  }

  /** GET /studies — list of study IDs. */
  public List<String> getStudies() {
    return getIdList("/studies");
  }

  /** GET /series — list of series IDs. */
  public List<String> getSeriesList() {
    return getIdList("/series");
  }

  /** GET /instances — list of instance IDs. */
  public List<String> getInstances() {
    return getIdList("/instances");
  }

  /** GET /instances/{id} — Orthanc instance metadata (JSON). */
  public Map<String, Object> getInstance(String instanceId) {
    validateOrthancInstanceId(instanceId);
    return getOrthancMap("/instances/" + instanceId);
  }

  /**
   * Streams raw DICOM bytes from Orthanc {@code GET /instances/{id}/file} (WADO-RS style).
   * Caller supplies the output stream (e.g. from {@link org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody}).
   */
  public void streamInstanceFileTo(String instanceId, OutputStream outputStream) {
    requireConfigured();
    validateOrthancInstanceId(instanceId);
    String url = baseUrl() + "/instances/" + instanceId + "/file";
    log.info("Orthanc streaming GET {}", url);
    int maxAttempts = orthanc.getMaxRetries() + 1;
    long baseDelay = orthanc.getRetryDelayMs();
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        restTemplate.execute(
            URI.create(url),
            HttpMethod.GET,
            request -> {
              HttpHeaders h = authHeadersForBinary();
              h.forEach((key, values) -> values.forEach(v -> request.getHeaders().add(key, v)));
            },
            response -> {
              if (!response.getStatusCode().is2xxSuccessful()) {
                throw new PacsConnectionException("Orthanc returned HTTP " + response.getStatusCode());
              }
              try (InputStream in = response.getBody()) {
                if (in != null) {
                  in.transferTo(outputStream);
                }
              }
              return null;
            });
        return;
      } catch (ResourceAccessException e) {
        log.warn("Orthanc stream attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
        if (attempt >= maxAttempts) {
          throw mapResourceAccess(url, e);
        }
        sleepMs(baseDelay * attempt);
      } catch (HttpClientErrorException e) {
        throw mapHttpClient("GET /instances/.../file", url, e);
      } catch (HttpServerErrorException e) {
        throw mapHttpServer("GET /instances/.../file", e);
      } catch (PacsConnectionException e) {
        throw e;
      } catch (Exception e) {
        throw new PacsConnectionException("Orthanc stream failed: " + e.getMessage(), e);
      }
    }
  }

  private static void validateOrthancInstanceId(String instanceId) {
    if (instanceId == null || !ORTHANC_INSTANCE_ID.matcher(instanceId).matches()) {
      throw new PacsConnectionException("Invalid Orthanc instance id format");
    }
  }

  public Map<String, Object> getPatient(String patientId) {
    return getOrthancMap("/patients/" + patientId);
  }

  public Map<String, Object> getSeries(String seriesId) {
    return getOrthancMap("/series/" + seriesId);
  }

  public Map<String, Object> getStudy(String studyId) {
    return getOrthancMap("/studies/" + studyId);
  }

  /** GET /system — Orthanc system info. */
  public Map<String, Object> getSystemInfo() {
    return getOrthancMap("/system");
  }

  public boolean isReachable() {
    OrthancHealthResult h = checkHealth();
    return "UP".equals(h.status());
  }

  public OrthancHealthResult checkHealth() {
    String displayUrl = orthanc.normalizedBaseUrl();
    if (!orthanc.isEnabled()) {
      log.info("Orthanc health: disabled via orthanc.enabled=false");
      return OrthancHealthResult.down("Orthanc integration is disabled (orthanc.enabled=false).", displayUrl);
    }
    if (!orthanc.isConfigured()) {
      log.warn("Orthanc health: URL not configured");
      return OrthancHealthResult.down(
          "Orthanc URL is not set. Configure orthanc.url or environment variable ORTHANC_URL. "
              + "On Render or other clouds, localhost does not reach Orthanc on your laptop — use a public URL.",
          displayUrl);
    }

    String url = baseUrl() + "/system";
    log.info("Orthanc health check: GET {}", url);
    try {
      HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("Orthanc health: UP");
        return OrthancHealthResult.up(displayUrl);
      }
      log.warn("Orthanc health: unexpected status {}", response.getStatusCode());
      return OrthancHealthResult.down("Unexpected HTTP status from Orthanc: " + response.getStatusCode(), displayUrl);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
        log.warn("Orthanc health: unauthorized");
        return OrthancHealthResult.down(
            "Orthanc returned 401 Unauthorized — verify orthanc.username and orthanc.password.", displayUrl);
      }
      log.warn("Orthanc health: HTTP {}", e.getStatusCode().value());
      return OrthancHealthResult.down(
          "Orthanc HTTP " + e.getStatusCode().value() + ": " + e.getStatusText(), displayUrl);
    } catch (HttpServerErrorException e) {
      log.error("Orthanc health: server error {}", e.getStatusCode());
      return OrthancHealthResult.down(
          "Orthanc server error " + e.getStatusCode().value() + ": " + e.getStatusText(), displayUrl);
    } catch (ResourceAccessException e) {
      PacsConnectionException mapped = mapResourceAccess(url, e);
      log.warn("Orthanc health: {}", mapped.getMessage());
      return OrthancHealthResult.down(mapped.getMessage(), displayUrl);
    } catch (Exception e) {
      log.error("Orthanc health check failed", e);
      return OrthancHealthResult.down(
          "Orthanc check failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
          displayUrl);
    }
  }
}
