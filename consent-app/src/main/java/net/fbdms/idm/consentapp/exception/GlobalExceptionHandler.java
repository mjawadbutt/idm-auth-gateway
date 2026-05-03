package net.fbdms.idm.consentapp.exception;

import jakarta.servlet.http.HttpServletRequest;
import net.fbdms.idm.common.exception.TenantResolutionException;
import net.fbdms.idm.consentapp.model.ErrorControllerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Translates exceptions into standardised {@link ErrorControllerResponse} JSON
 * responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(TenantResolutionException.class)
  public ResponseEntity<ErrorControllerResponse> handleTenantResolutionException(
      final TenantResolutionException ex,
      final HttpServletRequest request) {
    LOGGER.debug("Tenant resolution failed: {}", ex.getMessage());
    return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(HttpClientErrorException.class)
  public ResponseEntity<ErrorControllerResponse> handleHydraClientError(
      final HttpClientErrorException ex,
      final HttpServletRequest request) {
    LOGGER.warn("Hydra admin API client error: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
    return buildResponse(
        HttpStatus.valueOf(ex.getStatusCode().value()),
        "Hydra Error",
        "Hydra admin API returned an error",
        request.getRequestURI());
  }

  @ExceptionHandler(HttpServerErrorException.class)
  public ResponseEntity<ErrorControllerResponse> handleHydraServerError(
      final HttpServerErrorException ex,
      final HttpServletRequest request) {
    LOGGER.error("Hydra admin API server error: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
    return buildResponse(HttpStatus.BAD_GATEWAY, "Bad Gateway", "Hydra admin API is unavailable",
        request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorControllerResponse> handleUnexpectedException(
      final Exception ex,
      final HttpServletRequest request) {
    LOGGER.error("Unexpected error at path={}", request.getRequestURI(), ex);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
        "An unexpected error occurred", request.getRequestURI());
  }

  private ResponseEntity<ErrorControllerResponse> buildResponse(
      final HttpStatus status,
      final String error,
      final String message,
      final String path) {
    return ResponseEntity.status(status)
        .body(new ErrorControllerResponse(status.value(), error, message, path));
  }
}
