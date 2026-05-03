package net.fbdms.idm.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

/**
 * Returned by {@code IdentityDirectoryService#resolveTenantContext}.
 * Carries the tenant-scoped roles and permissions injected as token claims at
 * consent time.
 */
@Builder
public record TenantContext(
    @NotNull String tenantId,
    @NotNull String tenantName,
    @NotNull List<String> roles,
    @NotNull List<String> permissions) {
}
