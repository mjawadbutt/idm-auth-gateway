package net.fbdms.idm.common.model;

import jakarta.validation.constraints.NotNull;

/**
 * Returned by {@code TenantSelectorService#selectTenant}.
 * Carries the resolved tenant ID and whether the selection required user
 * interaction.
 */
public record TenantSelectionResult(
    @NotNull String tenantId,
    boolean selectionRequired) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String tenantId;
    private boolean selectionRequired;

    private Builder() {
    }

    public Builder tenantId(final String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public Builder selectionRequired(final boolean selectionRequired) {
      this.selectionRequired = selectionRequired;
      return this;
    }

    public TenantSelectionResult build() {
      return new TenantSelectionResult(tenantId, selectionRequired);
    }
  }
}
