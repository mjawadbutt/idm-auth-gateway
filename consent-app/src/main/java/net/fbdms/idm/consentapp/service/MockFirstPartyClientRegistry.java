package net.fbdms.idm.consentapp.service;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Local dev mock — treats the clients registered in
 * {@code hydra/clients/dev-clients.json} as first-party.
 * Never use in production.
 */
public class MockFirstPartyClientRegistry implements FirstPartyClientRegistry {

  private static final Set<String> FIRST_PARTY_CLIENT_IDS = Set.of(
      "test-saas-app",
      "idm-admin-console");

  @Override
  public boolean isFirstParty(final @NotNull String clientId) {
    return FIRST_PARTY_CLIENT_IDS.contains(clientId);
  }
}
