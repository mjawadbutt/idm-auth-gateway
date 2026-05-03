package net.fbdms.idm.common.model;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Returned by {@code IdentityDirectoryService#resolveTenantContext}.
 * Carries the tenant-scoped roles and permissions injected as token claims at
 * consent time.
 */
public record TenantContext(
    @NotNull String tenantId,
    @NotNull String tenantName,
    @NotNull List<String> roles,
    @NotNull List<String> permissions) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String tenantId;
    private String tenantName;
    private List<String> roles;
    private List<String> permissions;

    private Builder() {
    }

    public Builder tenantId(final String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public Builder tenantName(final String tenantName) {
      this.tenantName = tenantName;
      return this;
    }

    public Builder roles(final List<String> roles) {
      this.roles = roles;
      return this;
    }

    public Builder permissions(final List<String> permissions) {
      this.permissions = permissions;
      return this;
    }

    public TenantContext build() {
      return new TenantContext(tenantId, tenantName, roles, permissions);
    }
  }
}
