package iudx.file.server.authenticator.authorization;

import static iudx.file.server.authenticator.authorization.Api.DELETE_FILE;
import static iudx.file.server.authenticator.authorization.Api.UPLOAD;
import static iudx.file.server.authenticator.authorization.Method.DELETE;
import static iudx.file.server.authenticator.authorization.Method.POST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;
import iudx.file.server.authenticator.utilities.JwtData;

public class ProviderAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> providerAuthorizationRules = new HashMap<>();
  static {

    // file access list/rules
    List<AuthorizationRequest> fileAccessList = new ArrayList<>();
    fileAccessList.add(new AuthorizationRequest(POST, UPLOAD));
    fileAccessList.add(new AuthorizationRequest(DELETE, DELETE_FILE));
    providerAuthorizationRules.put("file", fileAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }

}
