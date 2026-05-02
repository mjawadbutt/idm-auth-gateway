package com.fbdms.idm.common.exception;

/**
 * Thrown by {@code IdentityDirectoryService#verifyCredentials} when credentials
 * are invalid
 * or the account is locked/disabled.
 */
public class AuthenticationException extends RuntimeException {

  public AuthenticationException(final String message) {
    super(message);
  }

  public AuthenticationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
