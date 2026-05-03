package net.fbdms.idm.loginapp.hydra.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for PUT /admin/oauth2/auth/requests/login/accept.
 */
public record HydraLoginAcceptRequest(
    @JsonProperty("subject") String subject,
    @JsonProperty("remember") boolean remember,
    @JsonProperty("remember_for") long rememberFor,
    @JsonProperty("context") Object context) {
}
