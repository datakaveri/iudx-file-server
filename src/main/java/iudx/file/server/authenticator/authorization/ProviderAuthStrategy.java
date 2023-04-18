package iudx.file.server.authenticator.authorization;

import static iudx.file.server.authenticator.authorization.Method.DELETE;
import static iudx.file.server.authenticator.authorization.Method.POST;

import iudx.file.server.authenticator.utilities.JwtData;
import iudx.file.server.common.Api;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderAuthStrategy implements AuthorizationStrategy {

  static Map<String, List<AuthorizationRequest>> providerAuthorizationRules = new HashMap<>();
  private static volatile ProviderAuthStrategy instance;

  private ProviderAuthStrategy(Api api) {
    buildPermissions(api);
  }

  public static ProviderAuthStrategy getInstance(Api apis) {
    if (instance == null) {
      synchronized (ProviderAuthStrategy.class) {
        if (instance == null) {
          instance = new ProviderAuthStrategy(apis);
        }
      }
    }
    return instance;
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
