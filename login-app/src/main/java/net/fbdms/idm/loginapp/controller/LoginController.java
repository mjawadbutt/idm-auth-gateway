package net.fbdms.idm.loginapp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import net.fbdms.idm.loginapp.hydra.model.HydraLoginRequest;
import net.fbdms.idm.loginapp.model.LoginControllerRequest;
import net.fbdms.idm.loginapp.model.LoginControllerResponse;
import net.fbdms.idm.loginapp.model.MfaVerifyControllerRequest;
import net.fbdms.idm.loginapp.model.TenantSelectControllerRequest;
import net.fbdms.idm.loginapp.service.LoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static net.fbdms.idm.loginapp.LoginAppApplication.ROOT_ENDPOINT_PREFIX;

/**
 * Handles the Hydra login challenge lifecycle.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>GET /idm-auth-gateway/login — fetch challenge details (called on page
 * load)</li>
 * <li>POST /idm-auth-gateway/login — submit credentials</li>
 * <li>POST /idm-auth-gateway/login/tenant — submit tenant selection</li>
 * <li>POST /idm-auth-gateway/login/mfa — submit MFA response</li>
 * </ul>
 */
@RestController
@RequestMapping(ROOT_ENDPOINT_PREFIX + "/login")
@Validated
public class LoginController {

  private final LoginService loginService;

  public LoginController(final LoginService loginService) {
    this.loginService = loginService;
  }

  /**
   * Called by the React app on page load to retrieve challenge details
   * (e.g. client name to display on the login form).
   */
  @GetMapping
  public ResponseEntity<HydraLoginRequest> getLoginRequest(
      @RequestParam("login_challenge") @NotBlank final String loginChallenge) {
    return ResponseEntity.ok(loginService.fetchLoginRequest(loginChallenge));
  }

  /**
   * Called when the user submits the login form.
   */
  @PostMapping
  public ResponseEntity<LoginControllerResponse> submitLogin(
      @RequestBody @Valid final LoginControllerRequest request) {
    return ResponseEntity.ok(loginService.processLogin(request));
  }

  /**
   * Called when the user selects a tenant from the tenant selector screen.
   */
  @PostMapping("/tenant")
  public ResponseEntity<LoginControllerResponse> submitTenantSelection(
      @RequestBody @Valid final TenantSelectControllerRequest request) {
    return ResponseEntity.ok(loginService.processTenantSelection(request));
  }

  /**
   * Called when the user submits their MFA response.
   */
  @PostMapping("/mfa")
  public ResponseEntity<LoginControllerResponse> submitMfaVerification(
      @RequestBody @Valid final MfaVerifyControllerRequest request) {
    return ResponseEntity.ok(loginService.processMfaVerification(request));
  }
}
