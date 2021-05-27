package iudx.file.server.authenticator;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.file.server.common.service.CatalogueService;

@VertxGen
@ProxyGen
public interface AuthenticationService {

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return AuthenticationServiceVertxEBProxy which is a service proxy
   */

  @GenIgnore
  static AuthenticationService createProxy(Vertx vertx, String address) {
    return new AuthenticationServiceVertxEBProxy(vertx, address);
  }

  /**
   * The tokenInterospect method implements the authentication and authorization module using IUDX
   * APIs. It caches the result of the TIP from the auth server for a duration specified by the
   * Constants TIP_CACHE_TIMEOUT_AMOUNT and TIP_CACHE_TIMEOUT_UNIT.
   *
   * @param request which is a JsonObject containing ids: [String]
   * @param authenticationInfo which is a JsonObject containing token: String and apiEndpoint:
   *        String
   * @param handler which is a request handler
   * @return AuthenticationService which is a service
   */

  @Fluent
  AuthenticationService tokenInterospect(JsonObject request,
      JsonObject authenticationInfo,
      Handler<AsyncResult<JsonObject>> handler);
}
