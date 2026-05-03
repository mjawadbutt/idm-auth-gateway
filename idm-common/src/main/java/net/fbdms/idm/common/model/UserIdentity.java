package net.fbdms.idm.common.model;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Returned by {@code IdentityDirectoryService#verifyCredentials}.
 * Represents the verified user and their tenant memberships.
 */
public record UserIdentity(
    @NotNull String userId,
    @NotNull String username,
    @NotNull String email,
    @NotNull List<String> tenantIds,
    boolean mfaRequired) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String userId;
    private String username;
    private String email;
    private List<String> tenantIds;
    private boolean mfaRequired;

    private Builder() {
    }

    public Builder userId(final String userId) {
      this.userId = userId;
      return this;
    }

    public Builder username(final String username) {
      this.username = username;
      return this;
    }

    public Builder email(final String email) {
      this.email = email;
      return this;
    }

    public Builder tenantIds(final List<String> tenantIds) {
      this.tenantIds = tenantIds;
      return this;
    }

    public Builder mfaRequired(final boolean mfaRequired) {
      this.mfaRequired = mfaRequired;
      return this;
    }

    public UserIdentity build() {
      return new UserIdentity(userId, username, email, tenantIds, mfaRequired);
    }
  }
}
