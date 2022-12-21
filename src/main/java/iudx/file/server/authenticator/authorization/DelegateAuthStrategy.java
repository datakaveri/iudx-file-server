package iudx.file.server.authenticator.authorization;

import static iudx.file.server.authenticator.authorization.Method.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iudx.file.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.file.server.authenticator.utilities.JwtData;

public class DelegateAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(DelegateAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> delegateAuthorizationRules = new HashMap<>();
  private final Api api;
  public DelegateAuthStrategy(Api api)
  {
    this.api = api;
    buildPermission(api);
  }
  private void buildPermission(Api api) {
    // file access list/rules
    List<AuthorizationRequest> fileAccessList = new ArrayList<>();
    fileAccessList.add(new AuthorizationRequest(POST, api.getApiFileUpload()));
    fileAccessList.add(new AuthorizationRequest(DELETE, api.getApiFileDelete()));
    delegateAuthorizationRules.put("file", fileAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }
}
