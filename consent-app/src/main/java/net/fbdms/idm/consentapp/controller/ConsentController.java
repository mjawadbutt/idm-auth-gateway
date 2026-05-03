package net.fbdms.idm.consentapp.controller;

import jakarta.validation.constraints.NotBlank;
import net.fbdms.idm.consentapp.hydra.model.HydraConsentRequest;
import net.fbdms.idm.consentapp.model.ConsentControllerResponse;
import net.fbdms.idm.consentapp.service.ConsentService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static net.fbdms.idm.consentapp.ConsentAppApplication.ROOT_ENDPOINT_PREFIX;

/**
 * Handles the Hydra consent challenge lifecycle.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>GET /idm-auth-gateway/consent — fetch challenge details (called on page
 * load)</li>
 * <li>POST /idm-auth-gateway/consent — process consent decision</li>
 * </ul>
 *
 * <p>
 * For first-party clients the POST is called immediately by the React app on
 * page load
 * (no user interaction required). For third-party clients (future) the GET is
 * used to
 * render a consent UI before the POST is submitted.
 */
@RestController
@RequestMapping(ROOT_ENDPOINT_PREFIX + "/consent")
@Validated
public class ConsentController {

  private final ConsentService consentService;

  public ConsentController(final ConsentService consentService) {
    this.consentService = consentService;
  }

  /**
   * Called by the React app on page load to retrieve consent challenge details.
   * Used to render the consent UI for third-party clients (future).
   */
  @GetMapping
  public ResponseEntity<HydraConsentRequest> getConsentRequest(
      @RequestParam("consent_challenge") @NotBlank final String consentChallenge) {
    return ResponseEntity.ok(consentService.fetchConsentRequest(consentChallenge));
  }

  /**
   * Called to process the consent decision.
   * For first-party clients this is called immediately without user interaction.
   */
  @PostMapping
  public ResponseEntity<ConsentControllerResponse> processConsent(
      @RequestParam("consent_challenge") @NotBlank final String consentChallenge) {
    return ResponseEntity.ok(consentService.processConsent(consentChallenge));
  }
}
