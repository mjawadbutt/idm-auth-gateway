package net.fbdms.idm.consentapp.service;

import jakarta.validation.constraints.NotNull;

/**
 * Determines whether an OAuth2 client is a first-party client.
 *
 * <p>
 * First-party clients (e.g. the IDM Admin Console, internal SaaS products owned
 * by FBDMS)
 * receive auto-accepted consent — no UI is shown to the user.
 * Third-party clients require explicit user consent (future implementation).
 *
 * <p>
 * Implementations may consult a static allowlist, a database, or Hydra client
 * metadata.
 */
public interface FirstPartyClientRegistry {

  /**
   * Returns {@code true} if the given client ID is a registered first-party
   * client.
   *
   * @param clientId the OAuth2 client ID to check
   */
  boolean isFirstParty(@NotNull String clientId);
}
