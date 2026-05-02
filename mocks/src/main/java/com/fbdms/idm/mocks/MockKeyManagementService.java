package com.fbdms.idm.mocks;

import com.fbdms.idm.common.service.KeyManagementService;
import jakarta.validation.constraints.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * Local dev mock — signs with a hardcoded dev key (HMAC-SHA256 placeholder).
 * Never use in production.
 */
public class MockKeyManagementService implements KeyManagementService {

  private static final byte[] DEV_KEY = "dev-signing-key-not-for-production".getBytes(StandardCharsets.UTF_8);

  @Override
  public byte @NotNull [] sign(final byte @NotNull [] payload) {
    // Trivial XOR-based mock signature — not cryptographically meaningful
    final byte[] signature = new byte[DEV_KEY.length];
    for (int i = 0; i < DEV_KEY.length; i++) {
      signature[i] = (byte) (DEV_KEY[i] ^ payload[i % payload.length]);
    }
    return signature;
  }

  @Override
  public void rotateSigningKey() {
    // no-op in dev
  }
}
