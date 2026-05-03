package net.fbdms.idm.consentapp.model;

/**
 * Response returned to the browser after a consent decision.
 * For first-party clients this is returned immediately with a redirect URL.
 * For third-party clients (future) it may signal that a consent UI is required.
 */
public record ConsentControllerResponse(
    String redirectTo,
    boolean consentUiRequired) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String redirectTo;
    private boolean consentUiRequired;

    private Builder() {
    }

    public Builder redirectTo(final String redirectTo) {
      this.redirectTo = redirectTo;
      return this;
    }

    public Builder consentUiRequired(final boolean consentUiRequired) {
      this.consentUiRequired = consentUiRequired;
      return this;
    }

    public ConsentControllerResponse build() {
      return new ConsentControllerResponse(redirectTo, consentUiRequired);
    }
  }
}
