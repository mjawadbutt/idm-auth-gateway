package net.fbdms.idm.loginapp.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.ProxySelector;

/**
 * Spring configuration for REST client beans.
 *
 * <p>
 * Uses Apache HttpClient 5 via {@link HttpComponentsClientHttpRequestFactory}.
 * A custom route planner ensures localhost calls to the Hydra admin API bypass
 * any corporate proxy.
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
    final CloseableHttpClient httpClient = HttpClients.custom()
        .disableAutomaticRetries()
        .disableRedirectHandling()
        .evictExpiredConnections()
        .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()) {
          @Override
          protected HttpHost determineProxy(
              final HttpHost target,
              final HttpContext context) throws HttpException {
            // -- Never proxy localhost — Hydra admin API is always local
            final String host = target.getHostName();
            if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
              return null;
            }
            return super.determineProxy(target, context);
          }
        })
        .build();

    return RestClient.builder()
        .baseUrl(applicationProperties.getHydraConfig().getAdminUrl())
        .defaultHeader("Accept", "application/json")
        .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
        .build();
  }
}
