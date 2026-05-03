package net.fbdms.idm.consentapp.hydra.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of Hydra's OAuth2 client representation returned inside consent
 * challenge responses.
 * The {@code metadata} field carries custom client attributes — used to
 * identify first-party clients.
 */
public record HydraConsentClient(
    @JsonProperty("client_id") String clientId,
    @JsonProperty("client_name") String clientName,
    @JsonProperty("metadata") Object metadata) {
}
