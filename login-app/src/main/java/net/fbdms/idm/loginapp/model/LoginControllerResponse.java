package net.fbdms.idm.loginapp.model;

import java.util.List;

/**
 * Response returned to the React client after a login attempt.
 *
 * <p>
 * Three outcomes are possible:
 * <ul>
 * <li>{@code redirectTo} non-null — login complete, browser should follow this
 * URL</li>
 * <li>{@code mfaRequired} true — MFA challenge issued, client should show MFA
 * screen</li>
 * <li>{@code tenantSelectionRequired} true — user has multiple tenants, client
 * should show selector</li>
 * </ul>
 */
public record LoginControllerResponse(
    String redirectTo,
    boolean mfaRequired,
    String mfaChallengeToken,
    String mfaHint,
    boolean tenantSelectionRequired,
    List<String> availableTenantIds,
    String userId,
    String tenantId) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String redirectTo;
    private boolean mfaRequired;
    private String mfaChallengeToken;
    private String mfaHint;
    private boolean tenantSelectionRequired;
    private List<String> availableTenantIds;
    private String userId;
    private String tenantId;

    private Builder() {
    }

    public Builder redirectTo(final String redirectTo) {
      this.redirectTo = redirectTo;
      return this;
    }

    public Builder mfaRequired(final boolean mfaRequired) {
      this.mfaRequired = mfaRequired;
      return this;
    }

    public Builder mfaChallengeToken(final String mfaChallengeToken) {
      this.mfaChallengeToken = mfaChallengeToken;
      return this;
    }

    public Builder mfaHint(final String mfaHint) {
      this.mfaHint = mfaHint;
      return this;
    }

    public Builder tenantSelectionRequired(final boolean tenantSelectionRequired) {
      this.tenantSelectionRequired = tenantSelectionRequired;
      return this;
    }

    public Builder availableTenantIds(final List<String> availableTenantIds) {
      this.availableTenantIds = availableTenantIds;
      return this;
    }

    public Builder userId(final String userId) {
      this.userId = userId;
      return this;
    }

    public Builder tenantId(final String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public LoginControllerResponse build() {
      return new LoginControllerResponse(
          redirectTo, mfaRequired, mfaChallengeToken, mfaHint,
          tenantSelectionRequired, availableTenantIds, userId, tenantId);
    }
  }
}
