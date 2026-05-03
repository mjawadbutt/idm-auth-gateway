package net.fbdms.idm.consentapp.hydra.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body from Hydra accept/reject endpoints.
 * Contains the redirect URL the browser must be sent to.
 */
public record HydraRedirectResponse(
    @JsonProperty("redirect_to") String redirectTo) {
}
