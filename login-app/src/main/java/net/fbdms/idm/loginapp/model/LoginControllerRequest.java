package net.fbdms.idm.loginapp.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body submitted by the React login form.
 */
public record LoginControllerRequest(
    @NotBlank String loginChallenge,
    @NotBlank String username,
    @NotBlank String password,
    String tenantId) {
}
