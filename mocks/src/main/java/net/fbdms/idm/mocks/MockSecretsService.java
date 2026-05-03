package net.fbdms.idm.mocks;

import net.fbdms.idm.common.service.SecretsService;
import jakarta.validation.constraints.NotNull;

/**
 * Local dev mock — reads secrets from environment variables.
 * Falls back to a hardcoded dev default if the variable is not set.
 * Never use in production.
 */
public class MockSecretsService implements SecretsService {

  @Override
  public @NotNull String getSecret(final @NotNull String key) {
    final String envValue = System.getenv(key);
    if (envValue != null && !envValue.isBlank()) {
      return envValue;
    }
    //-- Return a safe dev default so the app starts without any env setup
    return "dev-secret-for-" + key;
  }

  @Override
  public void putSecret(final @NotNull String key, final @NotNull String value) {
    //-- no-op — environment variables cannot be set programmatically at runtime
  }
}
