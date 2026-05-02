package com.fbdms.idm.common.service;

import com.fbdms.idm.common.dto.MfaChallenge;
import jakarta.validation.constraints.NotNull;

/**
 * Contract for IDM-5 (idm-auth-federation) — MFA trigger and verification.
 * The Login App orchestrates when to call this; IDM-5 provides the adapter
 * implementations.
 */
public interface MfaService {

  /**
   * Initiates an MFA challenge for the given user.
   *
   * @param userId the subject identifier of the user requiring MFA
   * @return the issued {@link MfaChallenge} containing the challenge token and
   *         method
   */
  @NotNull
  MfaChallenge triggerChallenge(@NotNull String userId);

  /**
   * Verifies the user's response to an MFA challenge.
   *
   * @param challengeToken the token issued by {@link #triggerChallenge}
   * @param userResponse   the OTP, WebAuthn assertion, or other response provided
   *                       by the user
   * @return {@code true} if the response is valid and the challenge is satisfied
   */
  boolean verifyChallenge(@NotNull String challengeToken, @NotNull String userResponse);
}
