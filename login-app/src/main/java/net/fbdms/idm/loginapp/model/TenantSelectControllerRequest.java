package net.fbdms.idm.loginapp.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body submitted by the React tenant selector screen.
 * The {@code availableTenantIds} are echoed back from the initial login
 * response
 * so the service can validate the selection without re-fetching identity.
 */
public record TenantSelectControllerRequest(
    @NotBlank String loginChallenge,
    @NotBlank String userId,
    @NotBlank String selectedTenantId,
    @NotNull List<String> availableTenantIds) {
}
