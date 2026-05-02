package com.fbdms.idm.common.service;

import jakarta.validation.constraints.NotNull;

/**
 * Abstraction over secrets retrieval and storage.
 * Provider implementations: AWS Secrets Manager, HashiCorp Vault, environment
 * variables (dev).
 * Wired via {@code @ConditionalOnProperty} — switching providers is a config
 * change only.
 */
public interface SecretsService {

  /**
   * Retrieves the secret value for the given key.
   *
   * @param key the secret identifier
   * @return the secret value
   * @throws IllegalArgumentException if the key does not exist
   */
  @NotNull
  String getSecret(@NotNull String key);

  /**
   * Stores or updates a secret value.
   *
   * @param key   the secret identifier
   * @param value the secret value to store
   */
  void putSecret(@NotNull String key, @NotNull String value);
}
