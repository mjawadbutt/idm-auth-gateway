package net.fbdms.idm.loginapp.hydra.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of Hydra's OAuth2 client representation returned inside challenge
 * responses.
 */
public record HydraClient(
    @JsonProperty("client_id") String clientId,
    @JsonProperty("client_name") String clientName) {
}
