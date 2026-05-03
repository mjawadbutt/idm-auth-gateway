package net.fbdms.idm.mocks;

import jakarta.validation.constraints.NotNull;
import net.fbdms.idm.common.exception.TenantResolutionException;
import net.fbdms.idm.common.model.TenantSelectionResult;
import net.fbdms.idm.common.service.TenantSelectorService;

import java.util.List;

/**
 * Local dev mock — auto-selects the first tenant for single-tenant users;
 * requires explicit selection for multi-tenant users.
 * Never use in production.
 */
public class MockTenantSelectorService implements TenantSelectorService {

  @Override
  public @NotNull TenantSelectionResult selectTenant(
      final @NotNull String userId,
      final @NotNull List<String> tenantIds,
      final String requestedTenantId) {

    if (tenantIds.isEmpty()) {
      throw new TenantResolutionException("User " + userId + " has no tenant memberships");
    }

    if (requestedTenantId != null) {
      if (!tenantIds.contains(requestedTenantId)) {
        throw new TenantResolutionException(
            "User " + userId + " is not a member of tenant " + requestedTenantId);
      }
      return TenantSelectionResult.builder()
          .tenantId(requestedTenantId)
          .selectionRequired(false)
          .build();
    }

    if (tenantIds.size() == 1) {
      return TenantSelectionResult.builder()
          .tenantId(tenantIds.get(0))
          .selectionRequired(false)
          .build();
    }

    //-- Multi-tenant user with no preference expressed — selection required
    return TenantSelectionResult.builder()
        .tenantId(tenantIds.get(0))
        .selectionRequired(true)
        .build();
  }
}
