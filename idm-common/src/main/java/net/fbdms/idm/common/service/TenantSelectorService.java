package net.fbdms.idm.common.service;

import jakarta.validation.constraints.NotNull;
import net.fbdms.idm.common.model.TenantSelectionResult;

import java.util.List;

/**
 * Determines which tenant a user operates in for the current login session.
 *
 * <p>
 * For single-tenant users the selection is automatic. For multi-tenant users
 * the implementation may require an explicit selection step (e.g. presenting a
 * picker UI or reading a previously stored preference).
 */
public interface TenantSelectorService {

  /**
   * Resolves the active tenant for the given user.
   *
   * <p>
   * If {@code requestedTenantId} is non-null the implementation must validate
   * that the user is a member of that tenant before accepting it.
   *
   * @param userId            the subject identifier of the authenticated user
   * @param tenantIds         the full list of tenant memberships for the user
   * @param requestedTenantId the tenant explicitly requested by the user, or
   *                          {@code null} if no preference was expressed
   * @return the resolved {@link TenantSelectionResult}
   * @throws net.fbdms.idm.common.exception.TenantResolutionException if
   *                                                                  {@code requestedTenantId}
   *                                                                  is provided
   *                                                                  but the user
   *                                                                  is not a
   *                                                                  member
   */
  @NotNull
  TenantSelectionResult selectTenant(
      @NotNull String userId,
      @NotNull List<String> tenantIds,
      String requestedTenantId);
}
