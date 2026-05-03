package net.fbdms.idm.loginapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures the {@link RestClient} instances used by the application.
 */
@Configuration
public class RestClientConfig {

  private final ApplicationProperties applicationProperties;

  public RestClientConfig(final ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  /**
   * RestClient pre-configured with the Hydra admin base URL.
   * Injected into {@code HydraAdminClient} by name.
   */
  @Bean
  public RestClient hydraAdminRestClient() {
    return RestClient.builder()
        .baseUrl(applicationProperties.getHydraConfig().getAdminUrl())
        .build();
  }
}
