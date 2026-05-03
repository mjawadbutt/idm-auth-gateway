package net.fbdms.idm.consentapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standardised error response returned by the {@code GlobalExceptionHandler}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorControllerResponse(
    int status,
    String error,
    String message,
    String path) {
}
