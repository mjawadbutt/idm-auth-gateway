package com.fbdms.idm.common.service;

import com.fbdms.idm.common.dto.TenantContext;
import com.fbdms.idm.common.dto.UserIdentity;
import jakarta.validation.constraints.NotNull;

/**
 * Contract for IDM-2 (idm-identity-store) — credential verification and
 * tenant/role resolution.
 * Implementations must target the fast read path; this is called in the
 * critical auth path.
 */
public interface IdentityDirectoryService {

  /**
   * Verifies the supplied credentials and returns the resolved user identity.
   *
   * @param username the username submitted at login
   * @param password the raw password submitted at login
   * @return the verified {@link UserIdentity}
   * @throws com.fbdms.idm.common.exception.AuthenticationException if credentials
   *                                                                are invalid
   */
  @NotNull
  UserIdentity verifyCredentials(@NotNull String username, @NotNull String password);

  /**
   * Resolves the tenant context (roles, permissions) for the given user and
   * tenant.
   * Called at consent time after the user has selected a tenant.
   *
   * @param userId   the subject identifier returned by {@link #verifyCredentials}
   * @param tenantId the tenant the user selected at login
   * @return the resolved {@link TenantContext}
   * @throws com.fbdms.idm.common.exception.TenantResolutionException if the user
   *                                                                  is not a
   *                                                                  member of
   *                                                                  the tenant
   */
  @NotNull
  TenantContext resolveTenantContext(@NotNull String userId, @NotNull String tenantId);
}
