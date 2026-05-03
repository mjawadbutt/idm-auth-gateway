package net.fbdms.idm.mocks;

import net.fbdms.idm.common.model.TenantContext;
import net.fbdms.idm.common.model.UserIdentity;
import net.fbdms.idm.common.exception.AuthenticationException;
import net.fbdms.idm.common.exception.TenantResolutionException;
import net.fbdms.idm.common.service.IdentityDirectoryService;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Local dev mock — returns hardcoded users, tenants, and roles.
 * Never use in production.
 */
public class MockIdentityDirectoryService implements IdentityDirectoryService {

  //-- username -> password (plaintext — dev only)
  private static final Map<String, String> USERS = Map.of(
      "alice", "password",
      "bob", "password");

  private static final Map<String, UserIdentity> IDENTITIES = Map.of(
      "alice", UserIdentity.builder()
          .userId("user-alice-001")
          .username("alice")
          .email("alice@dev.local")
          .tenantIds(List.of("tenant-acme", "tenant-globex"))
          .mfaRequired(false)
          .build(),
      "bob", UserIdentity.builder()
          .userId("user-bob-002")
          .username("bob")
          .email("bob@dev.local")
          .tenantIds(List.of("tenant-acme"))
          .mfaRequired(true)
          .build());

  @Override
  public @NotNull UserIdentity verifyCredentials(
      final @NotNull String username,
      final @NotNull String password) {
    final String expected = USERS.get(username);
    if (expected == null || !expected.equals(password)) {
      throw new AuthenticationException("Invalid credentials for user: " + username);
    }
    return IDENTITIES.get(username);
  }

  @Override
  public @NotNull TenantContext resolveTenantContext(
      final @NotNull String userId,
      final @NotNull String tenantId) {
    final boolean isMember = IDENTITIES.values().stream()
        .filter(u -> u.userId().equals(userId))
        .anyMatch(u -> u.tenantIds().contains(tenantId));

    if (!isMember) {
      throw new TenantResolutionException(
          "User " + userId + " is not a member of tenant " + tenantId);
    }

    return TenantContext.builder()
        .tenantId(tenantId)
        .tenantName(tenantId + " (mock)")
        .roles(List.of("ROLE_USER"))
        .permissions(List.of("read:own-data"))
        .build();
  }
}
