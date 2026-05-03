package net.fbdms.idm.loginapp.hydra.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from GET /admin/oauth2/auth/requests/login.
 * Contains the details of a pending Hydra login challenge.
 */
public record HydraLoginRequest(
    @JsonProperty("challenge") String challenge,
    @JsonProperty("client") HydraClient client,
    @JsonProperty("request_url") String requestUrl,
    @JsonProperty("requested_scope") String[] requestedScope,
    @JsonProperty("skip") boolean skip,
    @JsonProperty("subject") String subject) {
}
