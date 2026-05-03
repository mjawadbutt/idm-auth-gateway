package net.fbdms.idm.loginapp.hydra.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for PUT /admin/oauth2/auth/requests/login/reject.
 */
public record HydraLoginRejectRequest(
    @JsonProperty("error") String error,
    @JsonProperty("error_description") String errorDescription) {
}
