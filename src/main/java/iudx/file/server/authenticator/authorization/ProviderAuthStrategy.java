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

import io.vertx.core.json.JsonArray;
import iudx.file.server.authenticator.utilities.JwtData;

public class ProviderAuthStrategy implements AuthorizationStrategy {

    private volatile static ProviderAuthStrategy instance;
    static Map<String, List<AuthorizationRequest>> providerAuthorizationRules = new HashMap<>();

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
