package com.xray.backend.dto;

public record PatientRegistrationResponse(
    String patientUid,
    Long patientId,
    String patientName,
    String patientIdExternal,
    String message
) {}
