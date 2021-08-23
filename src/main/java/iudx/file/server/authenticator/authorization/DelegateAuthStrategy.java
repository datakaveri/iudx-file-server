package iudx.file.server.authenticator.authorization;

import static iudx.file.server.authenticator.authorization.Api.*;
import static iudx.file.server.authenticator.authorization.Method.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;
import iudx.file.server.authenticator.utilities.JwtData;

public class DelegateAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(DelegateAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> delegateAuthorizationRules = new HashMap<>();
  static {

    // file access list/rules
    List<AuthorizationRequest> fileAccessList = new ArrayList<>();
    fileAccessList.add(new AuthorizationRequest(POST, UPLOAD));
    fileAccessList.add(new AuthorizationRequest(DELETE, DELETE_FILE));
    delegateAuthorizationRules.put("file", fileAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    JsonArray access = jwtData.getCons() != null ? jwtData.getCons().getJsonArray("access") : null;
    boolean result = false;
    if (access == null) {
      return result;
    }
    String endpoint = authRequest.getApi().getApiEndpoint();
    Method method = authRequest.getMethod();
    LOGGER.info("authorization request for : " + endpoint + " with method : " + method.name());
    LOGGER.info("allowed access : " + access);

    if (access.contains("file")) {
      result = delegateAuthorizationRules.get("file").contains(authRequest);
    }
    
    return result;
  }
}
