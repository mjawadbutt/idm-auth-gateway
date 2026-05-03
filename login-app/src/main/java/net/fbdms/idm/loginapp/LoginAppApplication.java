package net.fbdms.idm.loginapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.fbdms.idm.loginapp.config.ApplicationProperties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.fbdms.iws.xwscommonlib.util.ApplicationBuildInfo;
import net.fbdms.iws.xwscommonlib.util.ApplicationRuntimeInfo;
import net.fbdms.iws.xwscommonlib.util.json.JacksonUtil;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class LoginAppApplication {

  public static final String ROOT_ENDPOINT_PREFIX = "/idm-auth-gateway";

  private static final Logger LOGGER = LoggerFactory.getLogger(LoginAppApplication.class);

  private final ApplicationProperties applicationProperties;
  private final ApplicationRuntimeInfo applicationRuntimeInfo;
  private final ApplicationBuildInfo applicationBuildInfo;
  private final ApplicationContext applicationContext;

  public LoginAppApplication(final ApplicationProperties applicationProperties,
      final ApplicationRuntimeInfo applicationRuntimeInfo,
      final ApplicationBuildInfo applicationBuildInfo,
      final ApplicationContext applicationContext) {
    this.applicationProperties = applicationProperties;
    this.applicationRuntimeInfo = applicationRuntimeInfo;
    this.applicationBuildInfo = applicationBuildInfo;
    this.applicationContext = applicationContext;
  }

  public static void main(final String[] args) {
    SpringApplication.run(LoginAppApplication.class, args);
  }

  @PostConstruct
  public void postConstruct() {
    LOGGER.debug("APPLICATION BUILD-INFO IS AS FOLLOWS:");
    LOGGER.debug("{}", getJacksonUtil().serializeObjectToJsonString(getApplicationBuildInfo()));
    LOGGER.debug("APPLICATION RUNTIME-INFO IS AS FOLLOWS:");
    LOGGER.debug("{}", getApplicationRuntimeInfo());
    LOGGER.debug("APPLICATION PROPERTIES ARE AS FOLLOWS:");
    LOGGER.debug("{}", getApplicationProperties());
    // TODO: Add support for tempDir, instanceid etc
    LOGGER.info("Application-context initialization completed successfully!");
  }

  @PreDestroy
  public void preDestroy() {
    LOGGER.info("Shutting down the application server.");
    // stopHsqlServer();
    LOGGER.info("Successfully shut down the application server.");
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = getJacksonUtil().getObjectMapper();
    return objectMapper;
  }

  public static JacksonUtil getJacksonUtil() {
    return JacksonUtil.getInstance();
  }

  public ApplicationProperties getApplicationProperties() {
    return applicationProperties;
  }

  public ApplicationRuntimeInfo getApplicationRuntimeInfo() {
    return applicationRuntimeInfo;
  }

  public ApplicationBuildInfo getApplicationBuildInfo() {
    return applicationBuildInfo;
  }

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

}
