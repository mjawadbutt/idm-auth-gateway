package net.fbdms.idm.consentapp.hydra.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Request body for PUT /admin/oauth2/auth/requests/consent/accept.
 */
public record HydraConsentAcceptRequest(
    @JsonProperty("grant_scope") List<String> grantScope,
    @JsonProperty("grant_access_token_audience") List<String> grantAccessTokenAudience,
    @JsonProperty("remember") boolean remember,
    @JsonProperty("remember_for") long rememberFor,
    @JsonProperty("session") HydraConsentSession session) {

  /**
   * Token session claims injected into the access token and ID token.
   */
  public record HydraConsentSession(
      @JsonProperty("access_token") Map<String, Object> accessToken,
      @JsonProperty("id_token") Map<String, Object> idToken) {
  }
}
