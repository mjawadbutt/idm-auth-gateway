package net.fbdms.idm.loginapp.config;

import net.fbdms.idm.common.service.AuditService;
import net.fbdms.idm.common.service.IdentityDirectoryService;
import net.fbdms.idm.common.service.MfaService;
import net.fbdms.idm.common.service.TenantSelectorService;
import net.fbdms.idm.mocks.MockAuditService;
import net.fbdms.idm.mocks.MockIdentityDirectoryService;
import net.fbdms.idm.mocks.MockMfaService;
import net.fbdms.idm.mocks.MockTenantSelectorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Wires mock implementations of all external service interfaces for local
 * development.
 * Active on the {@code local} profile (included in both {@code winLocal} and
 * {@code linuxLocal} profile groups).
 *
 * <p>
 * Replace each bean with a real implementation when the corresponding service
 * is available.
 */
@Configuration
@Profile("local")
public class MockBeansConfig {

  @Bean
  public IdentityDirectoryService identityDirectoryService() {
    return new MockIdentityDirectoryService();
  }

  @Bean
  public MfaService mfaService() {
    return new MockMfaService();
  }

  @Bean
  public TenantSelectorService tenantSelectorService() {
    return new MockTenantSelectorService();
  }

  @Bean
  public AuditService auditService() {
    return new MockAuditService();
  }
}
