package net.fbdms.idm.loginapp.hydra;

import jakarta.validation.constraints.NotNull;
import net.fbdms.idm.loginapp.hydra.model.HydraLoginAcceptRequest;
import net.fbdms.idm.loginapp.hydra.model.HydraLoginRejectRequest;
import net.fbdms.idm.loginapp.hydra.model.HydraLoginRequest;
import net.fbdms.idm.loginapp.hydra.model.HydraRedirectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin RestClient wrapper over the Hydra admin API endpoints used by the Login
 * App.
 *
 * <p>
 * Only the three endpoints required for the login challenge lifecycle are
 * implemented:
 * <ul>
 * <li>GET /admin/oauth2/auth/requests/login — fetch challenge details</li>
 * <li>PUT /admin/oauth2/auth/requests/login/accept — accept the challenge</li>
 * <li>PUT /admin/oauth2/auth/requests/login/reject — reject the challenge</li>
 * </ul>
 */
@Component
public class HydraAdminClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(HydraAdminClient.class);

  private static final String LOGIN_REQUEST_PATH = "/admin/oauth2/auth/requests/login";
  private static final String LOGIN_ACCEPT_PATH = "/admin/oauth2/auth/requests/login/accept";
  private static final String LOGIN_REJECT_PATH = "/admin/oauth2/auth/requests/login/reject";
  private static final String CHALLENGE_PARAM = "login_challenge";

  private final RestClient restClient;

  public HydraAdminClient(final RestClient hydraAdminRestClient) {
    this.restClient = hydraAdminRestClient;
  }

  /**
   * Fetches the details of a pending login challenge from Hydra.
   *
   * @param loginChallenge the challenge token received from Hydra via redirect
   * @return the {@link HydraLoginRequest} containing client and session details
   */
  public @NotNull HydraLoginRequest fetchLoginRequest(final @NotNull String loginChallenge) {
    LOGGER.debug("Fetching login request for challenge={}", loginChallenge);
    return restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(LOGIN_REQUEST_PATH)
            .queryParam(CHALLENGE_PARAM, loginChallenge)
            .build())
        .retrieve()
        .body(HydraLoginRequest.class);
  }

  /**
   * Accepts the login challenge, telling Hydra the user authenticated
   * successfully.
   *
   * @param loginChallenge the challenge token
   * @param acceptRequest  the accept payload containing the subject and session
   *                       context
   * @return the {@link HydraRedirectResponse} containing the URL to redirect the
   *         browser to
   */
  public @NotNull HydraRedirectResponse acceptLoginRequest(
      final @NotNull String loginChallenge,
      final @NotNull HydraLoginAcceptRequest acceptRequest) {
    LOGGER.debug("Accepting login request for challenge={}", loginChallenge);
    return restClient.put()
        .uri(uriBuilder -> uriBuilder
            .path(LOGIN_ACCEPT_PATH)
            .queryParam(CHALLENGE_PARAM, loginChallenge)
            .build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(acceptRequest)
        .retrieve()
        .body(HydraRedirectResponse.class);
  }

  /**
   * Rejects the login challenge, telling Hydra the login failed.
   *
   * @param loginChallenge the challenge token
   * @param rejectRequest  the reject payload containing the error details
   * @return the {@link HydraRedirectResponse} containing the URL to redirect the
   *         browser to
   */
  public @NotNull HydraRedirectResponse rejectLoginRequest(
      final @NotNull String loginChallenge,
      final @NotNull HydraLoginRejectRequest rejectRequest) {
    LOGGER.debug("Rejecting login request for challenge={}", loginChallenge);
    return restClient.put()
        .uri(uriBuilder -> uriBuilder
            .path(LOGIN_REJECT_PATH)
            .queryParam(CHALLENGE_PARAM, loginChallenge)
            .build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(rejectRequest)
        .retrieve()
        .body(HydraRedirectResponse.class);
  }
}
