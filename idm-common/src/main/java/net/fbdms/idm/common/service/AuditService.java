package net.fbdms.idm.common.service;

import jakarta.validation.constraints.NotNull;

/**
 * Contract for IDM-6 (idm-platform-ops) — audit event emission.
 * Implementations deliver events to the audit pipeline asynchronously.
 * The Login App calls this for login success, login failure, and MFA events.
 */
public interface AuditService {

  /**
   * Records a successful login.
   *
   * @param userId   the authenticated subject identifier
   * @param tenantId the tenant context selected at login
   * @param clientId the OAuth2 client that initiated the flow
   */
  void recordLoginSuccess(
      @NotNull String userId,
      @NotNull String tenantId,
      @NotNull String clientId);

  /**
   * Records a failed login attempt.
   *
   * @param username the username that was attempted
   * @param reason   a short description of the failure reason
   * @param clientId the OAuth2 client that initiated the flow
   */
  void recordLoginFailure(
      @NotNull String username,
      @NotNull String reason,
      @NotNull String clientId);

  /**
   * Records completion of an MFA challenge.
   *
   * @param userId    the subject identifier
   * @param method    the MFA method used (e.g. TOTP, WEBAUTHN)
   * @param succeeded whether the challenge was passed or failed
   */
  void recordMfaAttempt(
      @NotNull String userId,
      @NotNull String method,
      boolean succeeded);
}
