package iudx.file.server.authenticator.authorization;

import static iudx.file.server.authenticator.authorization.Method.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iudx.file.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;
import iudx.file.server.authenticator.utilities.JwtData;

public class DelegateAuthStrategy implements AuthorizationStrategy {
  private volatile static DelegateAuthStrategy instance;
  static Map<String, List<AuthorizationRequest>> delegateAuthorizationRules = new HashMap<>();
  private DelegateAuthStrategy(Api api)
  {
    buildPermissions(api);
  }
  public static DelegateAuthStrategy getInstance(Api apis)
  {
    if(instance == null)
    {
      synchronized (DelegateAuthStrategy.class)
      {
        if(instance == null)
        {
          instance = new DelegateAuthStrategy(apis);
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
    delegateAuthorizationRules.put("file", fileAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }
}
