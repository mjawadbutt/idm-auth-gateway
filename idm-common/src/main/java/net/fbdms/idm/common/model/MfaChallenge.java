package net.fbdms.idm.common.model;

import jakarta.validation.constraints.NotNull;

/**
 * MFA challenge/response contract between the Login App and IDM-5.
 */
public record MfaChallenge(
    @NotNull String challengeToken,
    @NotNull MfaMethod method,
    /**
     * Human-readable hint shown to the user (e.g. "Enter the code from your
     * authenticator app").
     */
    @NotNull String hint) {

  public enum MfaMethod {
    TOTP,
    WEBAUTHN,
    HARDWARE_KEY
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String challengeToken;
    private MfaMethod method;
    private String hint;

    private Builder() {
    }

    public Builder challengeToken(final String challengeToken) {
      this.challengeToken = challengeToken;
      return this;
    }

    public Builder method(final MfaMethod method) {
      this.method = method;
      return this;
    }

    public Builder hint(final String hint) {
      this.hint = hint;
      return this;
    }

    public MfaChallenge build() {
      return new MfaChallenge(challengeToken, method, hint);
    }
  }
}
