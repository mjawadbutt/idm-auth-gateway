package net.fbdms.idm.consentapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless security configuration.
 *
 * <p>
 * The consent-app does not own session state — Hydra owns the OAuth2 session.
 * Spring Security is configured to:
 * <ul>
 * <li>Disable HTTP sessions entirely</li>
 * <li>Disable CSRF (stateless API consumed by React)</li>
 * <li>Permit all requests to the consent endpoints</li>
 * <li>Permit actuator health/info endpoints</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
    http
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/consent/**").permitAll()
            .requestMatchers("/idm-auth-gateway/consent/**").permitAll()
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .anyRequest().denyAll());

    return http.build();
  }
}
