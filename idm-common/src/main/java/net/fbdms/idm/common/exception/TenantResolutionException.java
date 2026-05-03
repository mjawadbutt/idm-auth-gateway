package net.fbdms.idm.common.exception;

/**
 * Thrown by {@code IdentityDirectoryService#resolveTenantContext} when the user
 * is not a member of the requested tenant or the tenant does not exist.
 */
public class TenantResolutionException extends RuntimeException {

  public TenantResolutionException(final String message) {
    super(message);
  }

  public TenantResolutionException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
