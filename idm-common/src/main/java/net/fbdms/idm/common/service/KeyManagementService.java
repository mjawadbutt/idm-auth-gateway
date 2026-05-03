package net.fbdms.idm.common.service;

import jakarta.validation.constraints.NotNull;

/**
 * Abstraction over runtime cryptographic operations (signing, key rotation).
 * Provider implementations: AWS KMS, HashiCorp Vault, local dev key.
 * Wired via {@code @ConditionalOnProperty} — switching providers is a config
 * change only.
 */
public interface KeyManagementService {

  /**
   * Signs the given payload using the active signing key.
   *
   * @param payload the raw bytes to sign
   * @return the signature bytes
   */
  byte @NotNull [] sign(byte @NotNull [] payload);

  /**
   * Triggers rotation of the active signing key.
   * The previous key remains valid for token verification until its TTL expires.
   */
  void rotateSigningKey();
}
