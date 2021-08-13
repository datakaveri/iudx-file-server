package iudx.file.server.authenticator.authorization;

import iudx.file.server.authenticator.utilities.JwtData;

public interface AuthorizationStrategy {

  boolean isAuthorized(AuthorizationRequest authRequest,JwtData jwtData);

}
