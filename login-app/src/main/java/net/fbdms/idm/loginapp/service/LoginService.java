package net.fbdms.idm.loginapp.service;

import jakarta.validation.constraints.NotNull;
import net.fbdms.idm.common.exception.AuthenticationException;
import net.fbdms.idm.common.model.MfaChallenge;
import net.fbdms.idm.common.model.TenantSelectionResult;
import net.fbdms.idm.common.model.UserIdentity;
import net.fbdms.idm.common.service.AuditService;
import net.fbdms.idm.common.service.IdentityDirectoryService;
import net.fbdms.idm.common.service.MfaService;
import net.fbdms.idm.common.service.TenantSelectorService;
import net.fbdms.idm.loginapp.hydra.HydraAdminClient;
import net.fbdms.idm.loginapp.hydra.model.HydraLoginAcceptRequest;
import net.fbdms.idm.loginapp.hydra.model.HydraLoginRejectRequest;
import net.fbdms.idm.loginapp.hydra.model.HydraLoginRequest;
import net.fbdms.idm.loginapp.hydra.model.HydraRedirectResponse;
import net.fbdms.idm.loginapp.model.LoginControllerRequest;
import net.fbdms.idm.loginapp.model.LoginControllerResponse;
import net.fbdms.idm.loginapp.model.MfaVerifyControllerRequest;
import net.fbdms.idm.loginapp.model.TenantSelectControllerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orchestrates the Hydra login challenge lifecycle.
 *
 * <p>
 * Flow:
 * <ol>
 * <li>Fetch challenge from Hydra</li>
 * <li>Handle skip=true (existing session) — accept immediately</li>
 * <li>Verify credentials via {@link IdentityDirectoryService}</li>
 * <li>Resolve tenant via {@link TenantSelectorService} — show selector if
 * multi-tenant</li>
 * <li>Trigger MFA via {@link MfaService} if required (after tenant
 * selection)</li>
 * <li>Accept or reject the challenge via {@link HydraAdminClient}</li>
 * <li>Emit audit event via {@link AuditService}</li>
 * </ol>
 */
@Service
public class LoginService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoginService.class);

  private final HydraAdminClient hydraAdminClient;
  private final IdentityDirectoryService identityDirectoryService;
  private final TenantSelectorService tenantSelectorService;
  private final MfaService mfaService;
  private final AuditService auditService;

  public LoginService(
      final HydraAdminClient hydraAdminClient,
      final IdentityDirectoryService identityDirectoryService,
      final TenantSelectorService tenantSelectorService,
      final MfaService mfaService,
      final AuditService auditService) {
    this.hydraAdminClient = hydraAdminClient;
    this.identityDirectoryService = identityDirectoryService;
    this.tenantSelectorService = tenantSelectorService;
    this.mfaService = mfaService;
    this.auditService = auditService;
  }

  /**
   * Fetches the Hydra login challenge details.
   * Called when the browser first lands on the login page.
   *
   * @param loginChallenge the challenge token from the Hydra redirect
   * @return the Hydra login request details
   */
  public @NotNull HydraLoginRequest fetchLoginRequest(final @NotNull String loginChallenge) {
    return hydraAdminClient.fetchLoginRequest(loginChallenge);
  }

  /**
   * Processes a login form submission.
   *
   * <p>
   * If Hydra signals {@code skip=true} the user already has an active session and
   * the challenge must be
   * accepted immediately without re-verifying credentials.
   *
   * <p>
   * Otherwise: verifies credentials, resolves tenant (showing selector if
   * multi-tenant), then either
   * accepts the challenge or signals that further interaction is needed.
   * MFA is triggered after tenant selection, not before.
   *
   * @param request the login form submission
   * @return the outcome for the React client
   */
  public @NotNull LoginControllerResponse processLogin(final @NotNull LoginControllerRequest request) {
    final HydraLoginRequest hydraLoginRequest = hydraAdminClient.fetchLoginRequest(request.loginChallenge());

    // -- Hydra skip=true means the user already has an active session — accept
    // immediately.
    if (hydraLoginRequest.skip()) {
      LOGGER.debug("Hydra skip=true for challenge={}, accepting with existing subject={}",
          request.loginChallenge(), hydraLoginRequest.subject());
      return acceptAndRedirect(
          request.loginChallenge(),
          hydraLoginRequest.subject(),
          request.tenantId() != null ? request.tenantId() : "",
          hydraLoginRequest.client().clientId());
    }

    final UserIdentity identity;
    try {
      identity = identityDirectoryService.verifyCredentials(request.username(), request.password());
    } catch (AuthenticationException ex) {
      LOGGER.debug("Credential verification failed for username={}", request.username());
      rejectChallenge(request.loginChallenge(), "access_denied", ex.getMessage());
      auditService.recordLoginFailure(request.username(), ex.getMessage(),
          hydraLoginRequest.client().clientId());
      throw ex;
    }

    final TenantSelectionResult tenantResult = tenantSelectorService.selectTenant(
        identity.userId(), identity.tenantIds(), request.tenantId());

    // -- Multi-tenant user with no preference — show selector first, MFA comes
    // after.
    if (tenantResult.selectionRequired()) {
      return LoginControllerResponse.builder()
          .tenantSelectionRequired(true)
          .availableTenantIds(identity.tenantIds())
          .mfaRequired(identity.mfaRequired())
          .userId(identity.userId())
          .build();
    }

    // -- Single tenant (or preference already expressed) — check MFA next.
    if (identity.mfaRequired()) {
      final MfaChallenge mfaChallenge = mfaService.triggerChallenge(identity.userId());
      return LoginControllerResponse.builder()
          .mfaRequired(true)
          .mfaChallengeToken(mfaChallenge.challengeToken())
          .mfaHint(mfaChallenge.hint())
          .userId(identity.userId())
          .tenantId(tenantResult.tenantId())
          .build();
    }

    return acceptAndRedirect(
        request.loginChallenge(), identity.userId(), tenantResult.tenantId(),
        hydraLoginRequest.client().clientId());
  }

  /**
   * Processes a tenant selection after the user has chosen from the selector
   * screen.
   *
   * <p>
   * The client echoes back {@code userId}, {@code availableTenantIds}, and
   * {@code mfaRequired} from the initial login response — no re-verification of
   * credentials is performed.
   *
   * <p>
   * If MFA is required it is triggered here, after tenant selection, as per the
   * documented flow (step 7 follows step 6).
   *
   * @param request the tenant selection submission
   * @return the outcome for the React client (MFA challenge or redirect)
   */
  public @NotNull LoginControllerResponse processTenantSelection(
      final @NotNull TenantSelectControllerRequest request) {

    final TenantSelectionResult tenantResult = tenantSelectorService.selectTenant(
        request.userId(), request.availableTenantIds(), request.selectedTenantId());

    if (request.mfaRequired()) {
      final MfaChallenge mfaChallenge = mfaService.triggerChallenge(request.userId());
      return LoginControllerResponse.builder()
          .mfaRequired(true)
          .mfaChallengeToken(mfaChallenge.challengeToken())
          .mfaHint(mfaChallenge.hint())
          .userId(request.userId())
          .tenantId(tenantResult.tenantId())
          .build();
    }

    return acceptAndRedirect(
        request.loginChallenge(), request.userId(), tenantResult.tenantId(),
        extractClientId(request.loginChallenge()));
  }

  /**
   * Processes an MFA verification response.
   *
   * @param request the MFA verification submission
   * @return the outcome for the React client (redirect or rejection)
   */
  public @NotNull LoginControllerResponse processMfaVerification(
      final @NotNull MfaVerifyControllerRequest request) {

    final boolean verified = mfaService.verifyChallenge(request.challengeToken(), request.userResponse());
    final String clientId = extractClientId(request.loginChallenge());

    auditService.recordMfaAttempt(request.userId(), "TOTP", verified);

    if (!verified) {
      rejectChallenge(request.loginChallenge(), "access_denied", "MFA verification failed");
      auditService.recordLoginFailure(request.userId(), "MFA verification failed", clientId);
      throw new AuthenticationException("MFA verification failed");
    }

    return acceptAndRedirect(request.loginChallenge(), request.userId(), request.tenantId(), clientId);
  }

  private LoginControllerResponse acceptAndRedirect(
      final String loginChallenge,
      final String userId,
      final String tenantId,
      final String clientId) {

    final HydraLoginAcceptRequest acceptRequest = new HydraLoginAcceptRequest(
        userId,
        false,
        0,
        Map.of("tenant_id", tenantId));

    final HydraRedirectResponse redirect = hydraAdminClient.acceptLoginRequest(loginChallenge, acceptRequest);
    auditService.recordLoginSuccess(userId, tenantId, clientId);

    return LoginControllerResponse.builder()
        .redirectTo(redirect.redirectTo())
        .build();
  }

  private void rejectChallenge(
      final String loginChallenge,
      final String error,
      final String errorDescription) {
    hydraAdminClient.rejectLoginRequest(
        loginChallenge,
        new HydraLoginRejectRequest(error, errorDescription));
  }

  // -- Hydra challenge tokens are opaque — client ID is fetched from the
  // challenge details.
  // -- Used in paths where a second Hydra call is acceptable (tenant selection,
  // MFA audit path).
  private String extractClientId(final String loginChallenge) {
    try {
      return hydraAdminClient.fetchLoginRequest(loginChallenge).client().clientId();
    } catch (Exception ex) {
      LOGGER.debug("Could not extract clientId from challenge={}", loginChallenge);
      return "unknown";
    }
  }
}
