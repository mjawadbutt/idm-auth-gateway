package net.fbdms.idm.consentapp.hydra;

import jakarta.validation.constraints.NotNull;
import net.fbdms.idm.consentapp.hydra.model.HydraConsentAcceptRequest;
import net.fbdms.idm.consentapp.hydra.model.HydraConsentRejectRequest;
import net.fbdms.idm.consentapp.hydra.model.HydraConsentRequest;
import net.fbdms.idm.consentapp.hydra.model.HydraRedirectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin RestClient wrapper over the Hydra admin API endpoints used by the
 * Consent App.
 *
 * <p>
 * Endpoints covered:
 * <ul>
 * <li>GET /admin/oauth2/auth/requests/consent — fetch challenge details</li>
 * <li>PUT /admin/oauth2/auth/requests/consent/accept — accept with injected
 * claims</li>
 * <li>PUT /admin/oauth2/auth/requests/consent/reject — reject the
 * challenge</li>
 * </ul>
 */
@Component
public class HydraAdminClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(HydraAdminClient.class);

  private static final String CONSENT_REQUEST_PATH = "/admin/oauth2/auth/requests/consent";
  private static final String CONSENT_ACCEPT_PATH = "/admin/oauth2/auth/requests/consent/accept";
  private static final String CONSENT_REJECT_PATH = "/admin/oauth2/auth/requests/consent/reject";
  private static final String CHALLENGE_PARAM = "consent_challenge";

  private final RestClient restClient;

  public HydraAdminClient(final RestClient hydraAdminRestClient) {
    this.restClient = hydraAdminRestClient;
  }

  /**
   * Fetches the details of a pending consent challenge from Hydra.
   *
   * @param consentChallenge the challenge token received from Hydra via redirect
   * @return the {@link HydraConsentRequest} containing client, subject, and scope
   *         details
   */
  public @NotNull HydraConsentRequest fetchConsentRequest(final @NotNull String consentChallenge) {
    LOGGER.debug("Fetching consent request for challenge={}", consentChallenge);
    return restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(CONSENT_REQUEST_PATH)
            .queryParam(CHALLENGE_PARAM, consentChallenge)
            .build())
        .retrieve()
        .body(HydraConsentRequest.class);
  }

  /**
   * Accepts the consent challenge, injecting token claims into the session.
   *
   * @param consentChallenge the challenge token
   * @param acceptRequest    the accept payload containing granted scopes and
   *                         session claims
   * @return the {@link HydraRedirectResponse} containing the URL to redirect the
   *         browser to
   */
  public @NotNull HydraRedirectResponse acceptConsentRequest(
      final @NotNull String consentChallenge,
      final @NotNull HydraConsentAcceptRequest acceptRequest) {
    LOGGER.debug("Accepting consent request for challenge={}", consentChallenge);
    return restClient.put()
        .uri(uriBuilder -> uriBuilder
            .path(CONSENT_ACCEPT_PATH)
            .queryParam(CHALLENGE_PARAM, consentChallenge)
            .build())
        .body(acceptRequest)
        .retrieve()
        .body(HydraRedirectResponse.class);
  }

  /**
   * Rejects the consent challenge.
   *
   * @param consentChallenge the challenge token
   * @param rejectRequest    the reject payload containing the error details
   * @return the {@link HydraRedirectResponse} containing the URL to redirect the
   *         browser to
   */
  public @NotNull HydraRedirectResponse rejectConsentRequest(
      final @NotNull String consentChallenge,
      final @NotNull HydraConsentRejectRequest rejectRequest) {
    LOGGER.debug("Rejecting consent request for challenge={}", consentChallenge);
    return restClient.put()
        .uri(uriBuilder -> uriBuilder
            .path(CONSENT_REJECT_PATH)
            .queryParam(CHALLENGE_PARAM, consentChallenge)
            .build())
        .body(rejectRequest)
        .retrieve()
        .body(HydraRedirectResponse.class);
  }
}
