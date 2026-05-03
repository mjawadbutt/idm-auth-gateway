package net.fbdms.idm.consentapp.hydra.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for PUT /admin/oauth2/auth/requests/consent/reject.
 */
public record HydraConsentRejectRequest(
    @JsonProperty("error") String error,
    @JsonProperty("error_description") String errorDescription) {
}
