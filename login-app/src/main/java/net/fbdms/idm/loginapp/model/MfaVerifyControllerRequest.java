package net.fbdms.idm.loginapp.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body submitted by the React MFA screen.
 */
public record MfaVerifyControllerRequest(
    @NotBlank String loginChallenge,
    @NotBlank String userId,
    @NotBlank String tenantId,
    @NotBlank String challengeToken,
    @NotBlank String userResponse) {
}
