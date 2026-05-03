package net.fbdms.idm.consentapp.hydra.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from GET /admin/oauth2/auth/requests/consent.
 * Contains the details of a pending Hydra consent challenge.
 */
public record HydraConsentRequest(
    @JsonProperty("challenge") String challenge,
    @JsonProperty("client") HydraConsentClient client,
    @JsonProperty("requested_scope") String[] requestedScope,
    @JsonProperty("requested_access_token_audience") String[] requestedAccessTokenAudience,
    @JsonProperty("skip") boolean skip,
    @JsonProperty("subject") String subject,
    @JsonProperty("context") Object context) {
}
