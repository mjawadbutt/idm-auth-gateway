package net.fbdms.idm.loginapp.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

/**
 * Typed binding for all {@code application.*} properties.
 * Field names are auto-bound from matching property names — rename a field here
 * and rename the property too.
 */
@ConfigurationProperties(prefix = "application")
@Validated
public class ApplicationProperties {

  @Pattern(regexp = "^(winLocal|linuxLocal|p)$", message = "Invalid environmentId. Valid values: winLocal, linuxLocal, p")
  @NotBlank
  private final String environmentId;

  @NotNull
  private final Integer serverHttpPort;

  // Used to create application-context specific dirs or file name prefixes.
  // Defaults to Maven artifact ID via @project.artifactId@ filter.
  @NotBlank
  private final String dirOrFileName;

  private final String servletContextPath;

  @NotNull
  private final Integer sessionTimeoutInSeconds;

  @NotNull
  private final Integer actuatorPort;

  @NotNull
  private final HydraConfig hydraConfig;

  @NotNull
  private final MicroMeterConfig microMeterConfig;

  @NotNull
  private final LoggingConfig loggingConfig;

  @ConstructorBinding
  public ApplicationProperties(
      @NotBlank final String environmentId,
      @NotNull final Integer serverHttpPort,
      @NotBlank final String dirOrFileName,
      final String servletContextPath,
      @NotNull final Integer sessionTimeoutInSeconds,
      @NotNull final Integer actuatorPort,
      @NotNull final HydraConfig hydraConfig,
      @NotNull final MicroMeterConfig microMeterConfig,
      @NotNull final LoggingConfig loggingConfig) {
    this.environmentId = environmentId;
    this.serverHttpPort = serverHttpPort;
    this.dirOrFileName = dirOrFileName;
    this.servletContextPath = servletContextPath;
    this.sessionTimeoutInSeconds = sessionTimeoutInSeconds;
    this.actuatorPort = actuatorPort;
    this.hydraConfig = hydraConfig;
    this.microMeterConfig = microMeterConfig;
    this.loggingConfig = loggingConfig;
  }

  public String getEnvironmentId() {
    return environmentId;
  }

  public Integer getServerHttpPort() {
    return serverHttpPort;
  }

  public String getDirOrFileName() {
    return dirOrFileName;
  }

  public String getServletContextPath() {
    return servletContextPath;
  }

  public Integer getSessionTimeoutInSeconds() {
    return sessionTimeoutInSeconds;
  }

  public Integer getActuatorPort() {
    return actuatorPort;
  }

  public HydraConfig getHydraConfig() {
    return hydraConfig;
  }

  public MicroMeterConfig getMicroMeterConfig() {
    return microMeterConfig;
  }

  public LoggingConfig getLoggingConfig() {
    return loggingConfig;
  }

  // -------------------------------------------------------------------------

  public static class HydraConfig {

    @NotBlank
    private final String adminUrl;

    public HydraConfig(@NotBlank final String adminUrl) {
      this.adminUrl = adminUrl;
    }

    public String getAdminUrl() {
      return adminUrl;
    }
  }

  // -------------------------------------------------------------------------

  public static class LoggingConfig {

    @NotBlank
    private final String level;

    @NotNull
    private final Path rootDir;

    @NotBlank
    private final String mainLogFileName;

    @NotNull
    private final Path mainLogFilePath;

    @NotBlank
    private final String stacktraceLogFileName;

    @NotNull
    private final Path stacktraceLogFilePath;

    public LoggingConfig(
        @NotBlank final String level,
        @NotNull final Path rootDir,
        @NotBlank final String mainLogFileName,
        @NotNull final Path mainLogFilePath,
        @NotBlank final String stacktraceLogFileName,
        @NotNull final Path stacktraceLogFilePath) {
      this.level = level;
      this.rootDir = rootDir;
      this.mainLogFileName = mainLogFileName;
      this.mainLogFilePath = mainLogFilePath;
      this.stacktraceLogFileName = stacktraceLogFileName;
      this.stacktraceLogFilePath = stacktraceLogFilePath;
    }

    public String getLevel() {
      return level;
    }

    public Path getRootDir() {
      return rootDir;
    }

    public String getMainLogFileName() {
      return mainLogFileName;
    }

    public Path getMainLogFilePath() {
      return mainLogFilePath;
    }

    public String getStacktraceLogFileName() {
      return stacktraceLogFileName;
    }

    public Path getStacktraceLogFilePath() {
      return stacktraceLogFilePath;
    }
  }

  // -------------------------------------------------------------------------

  public static class MicroMeterConfig {

    private final boolean enableJmxMetrics;

    public MicroMeterConfig(final boolean enableJmxMetrics) {
      this.enableJmxMetrics = enableJmxMetrics;
    }

    public boolean isEnableJmxMetrics() {
      return enableJmxMetrics;
    }
  }
}
