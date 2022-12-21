package iudx.file.server.authenticator.authorization;

import static iudx.file.server.authenticator.authorization.Method.GET;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iudx.file.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;
import iudx.file.server.authenticator.utilities.JwtData;

public class ConsumerAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ConsumerAuthStrategy.class);
  private final Api api;
  static Map<String, List<AuthorizationRequest>> consumerAuthorizationRules = new HashMap<>();
  public ConsumerAuthStrategy(Api api)
  {
    this.api = api;
    buildPermissions(api);
  }

  private void buildPermissions(Api api) {
    // file access list/rules
    List<AuthorizationRequest> fileAccessList = new ArrayList<>();
    fileAccessList.add(new AuthorizationRequest(GET, api.getApiTemporal()));
    fileAccessList.add(new AuthorizationRequest(GET, api.getApiFileDownload()));
    fileAccessList.add(new AuthorizationRequest(GET, api.getListMetaData()));
    fileAccessList.add(new AuthorizationRequest(GET, api.getApiSpatial()));
    consumerAuthorizationRules.put("file", fileAccessList);

  }


  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    JsonArray access = jwtData.getCons() != null ? jwtData.getCons().getJsonArray("access") : null;
    boolean result = false;
    if (access == null) {
      return result;
    }
    String endpoint = authRequest.getApi();
    Method method = authRequest.getMethod();
    LOGGER.info("authorization request for : " + endpoint + " with method : " + method.name());
    LOGGER.info("allowed access : " + access);

    if (!result && access.contains("file")) {
      LOGGER.info("authRequest : " + authRequest);
      result = consumerAuthorizationRules.get("file").contains(authRequest);
    }
    LOGGER.info("result : " + result);
    return result;
  }

}
