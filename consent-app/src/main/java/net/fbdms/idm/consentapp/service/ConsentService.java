package net.fbdms.idm.consentapp.service;

import jakarta.validation.constraints.NotNull;
import net.fbdms.idm.common.exception.TenantResolutionException;
import net.fbdms.idm.common.model.TenantContext;
import net.fbdms.idm.common.service.AuditService;
import net.fbdms.idm.common.service.IdentityDirectoryService;
import net.fbdms.idm.consentapp.hydra.HydraAdminClient;
import net.fbdms.idm.consentapp.hydra.model.HydraConsentAcceptRequest;
import net.fbdms.idm.consentapp.hydra.model.HydraConsentAcceptRequest.HydraConsentSession;
import net.fbdms.idm.consentapp.hydra.model.HydraConsentRejectRequest;
import net.fbdms.idm.consentapp.hydra.model.HydraConsentRequest;
import net.fbdms.idm.consentapp.hydra.model.HydraRedirectResponse;
import net.fbdms.idm.consentapp.model.ConsentControllerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

/**
 * Orchestrates the Hydra consent challenge lifecycle.
 *
 * <p>
 * Flow:
 * <ol>
 * <li>Fetch consent challenge from Hydra</li>
 * <li>Handle skip=true — accept immediately with existing session claims</li>
 * <li>Resolve tenant context (roles, permissions) from IDM-2</li>
 * <li>Auto-accept for first-party clients, inject claims</li>
 * <li>Signal consent UI required for third-party clients (future)</li>
 * <li>Emit audit event via IDM-6</li>
 * </ol>
 */
@Service
public class ConsentService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsentService.class);

  private final HydraAdminClient hydraAdminClient;
  private final IdentityDirectoryService identityDirectoryService;
  private final FirstPartyClientRegistry firstPartyClientRegistry;
  private final AuditService auditService;

  public ConsentService(
      final HydraAdminClient hydraAdminClient,
      final IdentityDirectoryService identityDirectoryService,
      final FirstPartyClientRegistry firstPartyClientRegistry,
      final AuditService auditService) {
    this.hydraAdminClient = hydraAdminClient;
    this.identityDirectoryService = identityDirectoryService;
    this.firstPartyClientRegistry = firstPartyClientRegistry;
    this.auditService = auditService;
  }

  /**
   * Fetches the Hydra consent challenge details.
   * Called when the browser lands on the consent endpoint.
   *
   * @param consentChallenge the challenge token from the Hydra redirect
   * @return the Hydra consent request details
   */
  public @NotNull HydraConsentRequest fetchConsentRequest(final @NotNull String consentChallenge) {
    return hydraAdminClient.fetchConsentRequest(consentChallenge);
  }

  /**
   * Processes a consent challenge.
   *
   * <p>
   * If Hydra signals {@code skip=true} the user has previously granted consent
   * and it must be accepted immediately.
   *
   * <p>
   * For first-party clients consent is auto-accepted with full claim injection.
   * For third-party clients a consent UI is required (not yet implemented).
   *
   * @param consentChallenge the challenge token from the Hydra redirect
   * @return the outcome — redirect URL or signal that consent UI is needed
   */
  public @NotNull ConsentControllerResponse processConsent(final @NotNull String consentChallenge) {
    final HydraConsentRequest hydraConsentRequest = hydraAdminClient.fetchConsentRequest(consentChallenge);
    final String clientId = hydraConsentRequest.client().clientId();
    final String subject = hydraConsentRequest.subject();

    //-- skip=true means the user previously granted consent — accept immediately.
    if (hydraConsentRequest.skip()) {
      LOGGER.debug("Hydra skip=true for consent challenge={}, accepting immediately", consentChallenge);
      return acceptWithEmptySession(consentChallenge, hydraConsentRequest, subject, clientId);
    }

    if (!firstPartyClientRegistry.isFirstParty(clientId)) {
      //-- Third-party client — consent UI required (future implementation).
      LOGGER.debug("Third-party client={}, consent UI required", clientId);
      return ConsentControllerResponse.builder()
          .consentUiRequired(true)
          .build();
    }

    //-- First-party client — resolve tenant context and auto-accept.
    final String tenantId = extractTenantId(hydraConsentRequest);
    if (tenantId == null) {
      LOGGER.warn("No tenant_id in consent context for subject={} client={}", subject, clientId);
      rejectConsent(consentChallenge, "server_error", "Missing tenant context in login session");
      throw new TenantResolutionException("Missing tenant_id in Hydra login session context");
    }

    final TenantContext tenantContext;
    try {
      tenantContext = identityDirectoryService.resolveTenantContext(subject, tenantId);
    } catch (TenantResolutionException ex) {
      LOGGER.debug("Tenant resolution failed for subject={} tenantId={}", subject, tenantId);
      rejectConsent(consentChallenge, "access_denied", ex.getMessage());
      throw ex;
    }

    final ConsentControllerResponse response = acceptWithClaims(
        consentChallenge, hydraConsentRequest, subject, tenantContext);

    auditService.recordLoginSuccess(subject, tenantId, clientId);
    return response;
  }

  private ConsentControllerResponse acceptWithClaims(
      final String consentChallenge,
      final HydraConsentRequest hydraConsentRequest,
      final String subject,
      final TenantContext tenantContext) {

    final Map<String, Object> claims = Map.of(
        "tenant_id", tenantContext.tenantId(),
        "tenant_name", tenantContext.tenantName(),
        "roles", tenantContext.roles(),
        "permissions", tenantContext.permissions());

    final HydraConsentAcceptRequest acceptRequest = new HydraConsentAcceptRequest(
        Arrays.asList(hydraConsentRequest.requestedScope()),
        Arrays.asList(hydraConsentRequest.requestedAccessTokenAudience()),
        false,
        0,
        new HydraConsentSession(claims, claims));

    final HydraRedirectResponse redirect = hydraAdminClient.acceptConsentRequest(consentChallenge, acceptRequest);

    LOGGER.debug("Consent accepted for subject={} tenantId={}", subject, tenantContext.tenantId());
    return ConsentControllerResponse.builder()
        .redirectTo(redirect.redirectTo())
        .build();
  }

  private ConsentControllerResponse acceptWithEmptySession(
      final String consentChallenge,
      final HydraConsentRequest hydraConsentRequest,
      final String subject,
      final String clientId) {

    final HydraConsentAcceptRequest acceptRequest = new HydraConsentAcceptRequest(
        Arrays.asList(hydraConsentRequest.requestedScope()),
        Arrays.asList(hydraConsentRequest.requestedAccessTokenAudience()),
        false,
        0,
        new HydraConsentSession(Map.of(), Map.of()));

    final HydraRedirectResponse redirect = hydraAdminClient.acceptConsentRequest(consentChallenge, acceptRequest);

    LOGGER.debug("Consent skip-accepted for subject={} client={}", subject, clientId);
    return ConsentControllerResponse.builder()
        .redirectTo(redirect.redirectTo())
        .build();
  }

  private void rejectConsent(
      final String consentChallenge,
      final String error,
      final String errorDescription) {
    hydraAdminClient.rejectConsentRequest(
        consentChallenge,
        new HydraConsentRejectRequest(error, errorDescription));
  }

  //-- The tenant_id is injected into the Hydra login session context by the LoginApp.
  //-- It arrives here as part of the consent challenge's context field.
  private String extractTenantId(final HydraConsentRequest hydraConsentRequest) {
    final Object context = hydraConsentRequest.context();
    if (context instanceof Map<?, ?> contextMap) {
      final Object tenantId = contextMap.get("tenant_id");
      return tenantId instanceof String s ? s : null;
    }
    return null;
  }
}
