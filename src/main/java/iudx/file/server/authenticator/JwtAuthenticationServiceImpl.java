package iudx.file.server.authenticator;

import static iudx.file.server.authenticator.utilities.Constants.CACHE_TIMEOUT;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import iudx.file.server.authenticator.authorization.Api;
import iudx.file.server.authenticator.authorization.AuthorizationContextFactory;
import iudx.file.server.authenticator.authorization.AuthorizationRequest;
import iudx.file.server.authenticator.authorization.AuthorizationStrategy;
import iudx.file.server.authenticator.authorization.JwtAuthorization;
import iudx.file.server.authenticator.authorization.Method;
import iudx.file.server.authenticator.utilities.JwtData;
import iudx.file.server.cache.CacheService;
import iudx.file.server.cache.cacheImpl.CacheType;
import iudx.file.server.common.service.CatalogueService;


public class JwtAuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);


  final JWTAuth jwtAuth;
  final String host;
  final int port;;
  final String audience;
  final CatalogueService catalogueService;
  final CacheService cache;

  // resourceGroupCache will contains ACL info about all resource group in a resource server
  private final Cache<String, String> resourceGroupCache = CacheBuilder
      .newBuilder()
      .maximumSize(1000)
      .expireAfterAccess(CACHE_TIMEOUT, TimeUnit.MINUTES)
      .build();
  // resourceIdCache will contains info about resources available(& their ACL) in resource server.
  private final Cache<String, String> resourceIdCache = CacheBuilder
      .newBuilder()
      .maximumSize(1000)
      .expireAfterAccess(CACHE_TIMEOUT, TimeUnit.MINUTES)
      .build();

  JwtAuthenticationServiceImpl(Vertx vertx, final JWTAuth jwtAuth, final JsonObject config,
      final CatalogueService catalogueService, final CacheService cacheService) {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("host");
    this.catalogueService = catalogueService;
    this.cache = cacheService;
    host = config.getString("catalogueHost");
    port = config.getInteger("cataloguePort");

  }

  @Override
  public AuthenticationService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
      Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("token interospect called");
    String id = authenticationInfo.getString("id");;
    String token = authenticationInfo.getString("token");

    Future<JwtData> jwtDecodeFuture = decodeJwt(token);
    Future<Boolean> isItemExistFuture = catalogueService.isItemExist(id);


    ResultContainer result = new ResultContainer();

    jwtDecodeFuture.compose(decodeHandler -> {
      result.jwtData = decodeHandler;
      return isValidAudienceValue(result.jwtData);
    }).compose(audienceHandler -> {
      if (!result.jwtData.getIss().equals(result.jwtData.getSub())) {
        return isRevokedClientToken(result.jwtData);
      } else {
        return Future.succeededFuture(true);
      }
    }).compose(revokeTokenHandler -> {
      return isItemExistFuture;
    }).compose(itemExistHandler -> {
      return isValidId(result.jwtData, id);
    }).compose(validIdHandler -> {
      return validateAccess(result.jwtData, authenticationInfo);
    }).onComplete(completeHandler -> {
      LOGGER.debug("completion handler");
      if (completeHandler.succeeded()) {
        handler.handle(Future.succeededFuture(completeHandler.result()));
      } else {
        LOGGER.error("error : " + completeHandler.cause());
        handler.handle(Future.failedFuture(completeHandler.cause().getMessage()));
      }
    });

    return this;
  }

  // class to contain intermeddiate data for token interospection
  final class ResultContainer {
    JwtData jwtData;
    boolean isOpen;
  }


  Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();

    TokenCredentials creds = new TokenCredentials(jwtToken);

    jwtAuth.authenticate(creds)
        .onSuccess(user -> {
          JwtData jwtData = new JwtData(user.principal());
          promise.complete(jwtData);
        }).onFailure(err -> {
          LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
          promise.fail("failed");
        });

    return promise.future();
  }

  public Future<JsonObject> validateAccess(JwtData jwtData, JsonObject authInfo) {
    LOGGER.trace("validateAccess() started");
    Promise<JsonObject> promise = Promise.promise();

    Method method = Method.valueOf(authInfo.getString("method"));
    Api api = Api.fromEndpoint(authInfo.getString("apiEndpoint"));
    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);

    AuthorizationStrategy authStrategy = AuthorizationContextFactory.create(jwtData.getRole());
    LOGGER.info("strategy : " + authStrategy.getClass().getSimpleName());
    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    LOGGER.info("endPoint : " + authInfo.getString("apiEndpoint"));
    if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
      JsonObject jsonResponse = new JsonObject();
      jsonResponse.put("userID", jwtData.getSub());
      promise.complete(jsonResponse);
    } else {
      LOGGER.info("failed");
      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  Future<Boolean> isValidAudienceValue(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    if (audience != null && audience.equalsIgnoreCase(jwtData.getAud())) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect audience value in jwt");
      promise.fail("Incorrect audience value in jwt");
    }
    return promise.future();
  }

  Future<Boolean> isValidId(JwtData jwtData, String id) {
    Promise<Boolean> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];
    if (id.equalsIgnoreCase(jwtId)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect id value in jwt");
      promise.fail("Incorrect id value in jwt");
    }

    return promise.future();
  }

  Future<Boolean> isRevokedClientToken(JwtData jwtData) {
    LOGGER.debug("isRevokedClientToken started");
    Promise<Boolean> promise = Promise.promise();
    CacheType cacheType = CacheType.REVOKED_CLIENT;
    String subId = jwtData.getSub();
    JsonObject requestJson = new JsonObject().put("type", cacheType).put("key", subId);

    LOGGER.debug("requestJson : " + requestJson);
    cache.get(requestJson, handler -> {
      if (handler.succeeded()) {
        JsonObject responseJson = handler.result();
        LOGGER.debug("responseJson : " + responseJson);
        String timestamp = responseJson.getJsonArray("result").getJsonObject(0).getString("value");

        LocalDateTime revokedAt = LocalDateTime.parse(timestamp);
        LocalDateTime jwtIssuedAt = (LocalDateTime.ofInstant(
            Instant.ofEpochSecond(jwtData.getIat()),
            ZoneId.systemDefault()));
        if (jwtIssuedAt.isBefore(revokedAt)) {
          LOGGER.error("Privilages for client is revoked.");
          JsonObject result = new JsonObject().put("401", "revoked token passes");
          promise.fail(result.toString());
        } else {
          promise.complete(true);
        }
      } else {
        // since no value in cache, this means client_id is valie and not revoked
        LOGGER.debug("cache call result : [fail] " + handler.cause());
        promise.complete(true);
      }
    });
    return promise.future();
  }


}
