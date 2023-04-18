package iudx.file.server.authenticator.authorization;

import iudx.file.server.authenticator.utilities.JwtData;

public final class JwtAuthorization {
  private final AuthorizationStrategy authStrategy;

  public JwtAuthorization(final AuthorizationStrategy authStrategy) {
    this.authStrategy = authStrategy;
  }

  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return authStrategy.isAuthorized(authRequest, jwtData);
  }
}
