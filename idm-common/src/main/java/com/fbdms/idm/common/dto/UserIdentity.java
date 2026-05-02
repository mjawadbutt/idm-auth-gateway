package com.fbdms.idm.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

/**
 * Returned by {@code IdentityDirectoryService#verifyCredentials}.
 * Represents the verified user and their tenant memberships.
 */
@Builder
public record UserIdentity(
    @NotNull String userId,
    @NotNull String username,
    @NotNull String email,
    @NotNull List<String> tenantIds,
    boolean mfaRequired) {
}
