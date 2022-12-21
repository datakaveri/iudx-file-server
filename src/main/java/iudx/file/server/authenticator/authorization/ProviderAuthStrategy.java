package iudx.file.server.authenticator.authorization;


import static iudx.file.server.authenticator.authorization.Method.DELETE;
import static iudx.file.server.authenticator.authorization.Method.POST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iudx.file.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.file.server.authenticator.utilities.JwtData;

public class ProviderAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> providerAuthorizationRules = new HashMap<>();
  private final Api api;
  public ProviderAuthStrategy(Api api)
  {
    this.api = api;
    buildPermissions(api);
  }

  private void buildPermissions(Api api) {
    // file access list/rules
    List<AuthorizationRequest> fileAccessList = new ArrayList<>();
    fileAccessList.add(new AuthorizationRequest(POST, api.getApiFileUpload()));
    fileAccessList.add(new AuthorizationRequest(DELETE, api.getApiFileDelete()));
    providerAuthorizationRules.put("file", fileAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }

}
