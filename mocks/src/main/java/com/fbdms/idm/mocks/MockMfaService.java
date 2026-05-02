package com.fbdms.idm.mocks;

import com.fbdms.idm.common.dto.MfaChallenge;
import com.fbdms.idm.common.service.MfaService;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Local dev mock — always issues a TOTP challenge and always returns MFA
 * success.
 * Never use in production.
 */
public class MockMfaService implements MfaService {

  @Override
  public @NotNull MfaChallenge triggerChallenge(final @NotNull String userId) {
    return MfaChallenge.builder()
        .challengeToken(UUID.randomUUID().toString())
        .method(MfaChallenge.MfaMethod.TOTP)
        .hint("Enter any code — mock always succeeds")
        .build();
  }

  @Override
  public boolean verifyChallenge(
      final @NotNull String challengeToken,
      final @NotNull String userResponse) {
    return true;
  }
}
