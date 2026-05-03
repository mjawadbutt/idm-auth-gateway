package net.fbdms.idm.mocks;

import net.fbdms.idm.common.service.AuditService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local dev mock — logs audit events to SLF4J instead of emitting to IDM-6.
 * Never use in production.
 */
public class MockAuditService implements AuditService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MockAuditService.class);

  @Override
  public void recordLoginSuccess(
      final @NotNull String userId,
      final @NotNull String tenantId,
      final @NotNull String clientId) {
    LOGGER.info("[MOCK AUDIT] LOGIN_SUCCESS userId={} tenantId={} clientId={}", userId, tenantId, clientId);
  }

  @Override
  public void recordLoginFailure(
      final @NotNull String username,
      final @NotNull String reason,
      final @NotNull String clientId) {
    LOGGER.info("[MOCK AUDIT] LOGIN_FAILURE username={} reason={} clientId={}", username, reason, clientId);
  }

  @Override
  public void recordMfaAttempt(
      final @NotNull String userId,
      final @NotNull String method,
      final boolean succeeded) {
    LOGGER.info("[MOCK AUDIT] MFA_ATTEMPT userId={} method={} succeeded={}", userId, method, succeeded);
  }
}
