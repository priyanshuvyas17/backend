package com.xray.backend.utils;

import java.util.regex.Pattern;

/**
 * Validates identifiers for {@code GET /dicom/{id}} and Orthanc instance routes.
 */
public final class DicomResourceIdValidator {

  /** Local SOP / file token or Orthanc UUID-style instance id. */
  private static final Pattern SAFE_ID =
      Pattern.compile("^[a-zA-Z0-9._-]{1,128}$");

  private DicomResourceIdValidator() {}

  public static void validate(String id) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("DICOM resource id is required");
    }
    if (!SAFE_ID.matcher(id).matches()) {
      throw new IllegalArgumentException("Invalid DICOM resource id");
    }
  }

  /** Normalize local storage key (e.g. add .dcm if stored that way). */
  public static String normalizeSopKey(String id) {
    if (id == null) {
      return "";
    }
    String t = id.trim();
    if (t.endsWith(".dcm") || t.endsWith(".DCM")) {
      return t;
    }
    return t + ".dcm";
  }
}
