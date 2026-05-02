package com.fbdms.idm.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * MFA challenge/response contract between the Login App and IDM-5.
 */
@Builder
public record MfaChallenge(
    @NotNull String challengeToken,
    @NotNull MfaMethod method,
    /**
     * Human-readable hint shown to the user (e.g. "Enter the code from your
     * authenticator app").
     */
    @NotNull String hint) {

  public enum MfaMethod {
    TOTP,
    WEBAUTHN,
    HARDWARE_KEY
  }
}
