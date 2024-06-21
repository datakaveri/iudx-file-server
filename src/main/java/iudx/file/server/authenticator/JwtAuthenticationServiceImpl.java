package iudx.file.server.authenticator;
import iudx.file.server.apiserver.response.ResponseUrn;

import static iudx.file.server.authenticator.utilities.Constants.*;
import static iudx.file.server.common.Constants.CAT_SEARCH_PATH;

import io.vertx.core.json.JsonArray;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import iudx.file.server.authenticator.authorization.* ;
import iudx.file.server.authenticator.utilities.JwtData;
import iudx.file.server.cache.CacheService;
import iudx.file.server.cache.cacheimpl.CacheType;
import iudx.file.server.common.Api;
import iudx.file.server.common.Response;
import iudx.file.server.common.service.CatalogueService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.file.server.database.postgres.PostgresService;

public class JwtAuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);

  final JWTAuth jwtAuth;
  final String host;
  final int port;
  final String audience;
  final String path;
  final String catBasePath;
  final CatalogueService catalogueService;
  final CacheService cache;
  final PostgresService postgresService;
  final Api api;
  // resourceGroupCache will contains ACL info about all resource group in a resource server
  private final Cache<String, String> resourceGroupCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(CACHE_TIMEOUT, TimeUnit.MINUTES)
          .build();
  // resourceIdCache will contains info about resources available(& their ACL) in resource server.
  private final Cache<String, String> resourceIdCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(CACHE_TIMEOUT, TimeUnit.MINUTES)
          .build();
  WebClient catWebClient;

  JwtAuthenticationServiceImpl(
      Vertx vertx,
      final JWTAuth jwtAuth,
      final JsonObject config,
      final CatalogueService catalogueService,
      final CacheService cacheService,
      final PostgresService postgresService,
      final Api api
  )
  {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("audience");
    this.catalogueService = catalogueService;
    this.cache = cacheService;
    host = config.getString("catalogueHost");
    port = config.getInteger("cataloguePort");
    this.catBasePath = config.getString("dxCatalogueBasePath");
    this.path = catBasePath + CAT_SEARCH_PATH;
    this.postgresService= postgresService;
    this.api = api;

    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(true).setVerifyHost(false).setSsl(true);
    catWebClient = WebClient.create(vertx, options);
  }

  @Override
  public AuthenticationService tokenInterospect(
      JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("token interospect called");
    String id = authenticationInfo.getString("id");
    String token = authenticationInfo.getString("token");
    String endPoint = authenticationInfo.getString("apiEndpoint");

    boolean skipIdCheck =
        endPoint.equalsIgnoreCase(api.getApiFileUpload())
            || endPoint.equalsIgnoreCase(api.getApiFileDelete());

    LOGGER.info("token is" + token);
    LOGGER.info("the authenticationinfo look slike{}",authenticationInfo);
    LOGGER.info("the request looks like{}",request);

    Future<JwtData> jwtDecodeFuture = decodeJwt(token);
    Future<Boolean> isItemExistFuture = catalogueService.isItemExist(id);

    ResultContainer result = new ResultContainer();

    jwtDecodeFuture
        .compose(
            decodeHandler -> {
              result.jwtData = decodeHandler;

              return isValidAudienceValue(result.jwtData);
            })
        .compose(
            audienceHandler -> {
              if (!result.jwtData.getIss().equals(result.jwtData.getSub())) {
                return isRevokedClientToken(result.jwtData);
              } else {
                return Future.succeededFuture(true);
              }
            })
        .compose(
            revokeTokenHandler -> {
              return isItemExistFuture;
            })
        .compose(
            itemExistHandler -> {
              if (!result.jwtData.getIss().equals(result.jwtData.getSub())) {
                return isOpenResource(id);
              } else {
                return Future.succeededFuture("OPEN");
              }
            })
        .compose(
            openResourceHandler -> {
              result.isOpen = openResourceHandler.equalsIgnoreCase("OPEN");
              if (result.isOpen && checkOpenEndPoints(endPoint)) {
                return Future.succeededFuture(true);
              } else if (checkQueryEndPoints(endPoint)) {
                return Future.succeededFuture(true);
              } else if (!result.isOpen && !skipIdCheck) {
                return isValidId(result.jwtData, id);
              } else {
                return Future.succeededFuture(true);
              }
            })
        .compose(
            validIdHandler -> {
              if (result.jwtData.getIss().equals(result.jwtData.getSub())) {
                JsonObject jsonResponse = new JsonObject();
                jsonResponse.put(JSON_USERID, result.jwtData.getSub());
                LOGGER.info("jwt : " + result.jwtData);
                jsonResponse.put(
                    JSON_EXPIRY,
                    LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(
                                Long.parseLong(String.valueOf(result.jwtData.getExp()))),
                            ZoneId.systemDefault())
                        .toString());
                return Future.succeededFuture(jsonResponse);
              } else {
                return validateAccess(result.jwtData, result.isOpen, authenticationInfo);

              }
            })
//
      .compose(
        validateAccessHandler -> {


          return validateAccessRestriction(validateAccessHandler, result);
            //          return null;
        }
      )
        .onComplete(

            completeHandler -> {

              if (completeHandler.succeeded()) {
                handler.handle(Future.succeededFuture((JsonObject) completeHandler.result()));
              } else {
                LOGGER.error("error : " + completeHandler.cause());
                LOGGER.error("error : " + completeHandler);
                handler.handle(Future.failedFuture(completeHandler.cause().getMessage()));
              }
            });

    return this;
  }

  private Future<JsonObject> validateAccessRestriction(JsonObject authInfo, ResultContainer result) {
    Promise<JsonObject> promise = Promise.promise();
    String queryToExecute = DATA_ACCESS_RESTRICTION.replace("$1", result.jwtData.getSub());
    postgresService.executeQuery(queryToExecute, queryResultHandler -> {
      if(queryResultHandler.succeeded()) {
        JsonObject queryResult = queryResultHandler.result();
        JsonArray accessArray = result.jwtData.getCons().getJsonArray("access");

        if (accessArray != null && !accessArray.isEmpty()) {
          JsonObject firstAccessObject = accessArray.getJsonObject(0);
          if (firstAccessObject != null) {
            int fileLimit = Integer.parseInt((String) firstAccessObject.getValue("file", "0"));
            int apiLimit = Integer.parseInt((String) firstAccessObject.getValue("api", "0"));

            long numRows = (long) queryResult.getJsonArray("result").getJsonObject(0).getValue("num_rows", 0L);
            long totalDataDownloaded =  queryResult.getJsonArray("result").getJsonObject(0).getNumber("total_data_downloaded").longValue();




            if (numRows>apiLimit || totalDataDownloaded>fileLimit)  {
              promise.fail("fail"); // Access denied
            } else {
              promise.complete(authInfo); // Access granted
            }
          }
        } else {
          Response unauthorizedResponse = new Response.Builder()
            .withUrn(ResponseUrn.UNAUTHORIZED_URN.getUrn())
            .withStatus(HttpStatus.SC_UNAUTHORIZED)
            .withDetail("Access denied due to invalid access information")
            .build();
          promise.fail(unauthorizedResponse.toString());
        }
      } else {
        //prmise fail
        promise.fail("Invalid Query");
      }
    });
    return promise.future();
  }

  private boolean checkQueryEndPoints(String endPoint) {
    for (String item : QUERY_ENDPOINTS) {
      if (endPoint.contains(item)) {
        return true;
      }
    }
    return false;
  }

  private boolean checkOpenEndPoints(String endPoint) {
    for (String item : OPEN_ENDPOINTS) {
      if (endPoint.contains(item)) {
        return true;
      }
    }
    return false;
  }


  public Future<String> isOpenResource(String id) {
    LOGGER.trace("isOpenResource() started");
    Promise<String> promise = Promise.promise();

    String aclResorce = resourceIdCache.getIfPresent(id);
    String aclGroup = resourceGroupCache.getIfPresent(id);
    if (aclResorce != null) {
      LOGGER.debug("resourceIdCache Hit");
      promise.complete(aclResorce);
    } else if (aclGroup != null) {
      LOGGER.debug("resourceGroupCache Hit");
      promise.complete(aclGroup);
    } else {
      // cache miss
      LOGGER.debug("Cache miss calling cat server");

      Future<JsonObject> resourceAclFuture = catalogueService.getRelItem(id);
      resourceAclFuture
          .onSuccess(
              catResult -> {
                if (catResult.containsKey("accessPolicy")
                    && catResult.containsKey("resourceGroup")) {
                  resourceIdCache.put(id, catResult.getString("accessPolicy"));
                  promise.complete(catResult.getString("accessPolicy"));
                  return;
                }
                LOGGER.debug("policy not available in RL now checking in RG");
                String groupId =
                    catResult.getString("type").equalsIgnoreCase(ITEM_TYPE_RESOURCE)
                        ? catResult.getString("resourceGroup")
                        : catResult.getString("id");

                Future<JsonObject> groupAclFuture = catalogueService.getRelItem(groupId);
                groupAclFuture
                    .onSuccess(
                        grpACl -> {
                          if (grpACl.containsKey("accessPolicy")) {
                            resourceGroupCache.put(groupId, grpACl.getString("accessPolicy"));
                            promise.complete(grpACl.getString("accessPolicy"));
                          } else {
                            LOGGER.error("ACL not defined in group or resource item");
                            promise.fail("ACL not defined in group or resource item");
                          }
                        })
                    .onFailure(
                        filure -> {
                          LOGGER.info("cat server error " + filure.getMessage());
                          promise.fail("Not Found " + id);
                        });
              })
          .onFailure(
              filure -> {
                LOGGER.info("cat server error " + filure.getMessage());
                promise.fail("Not Found " + id);
              });
    }
    return promise.future();
  }

  Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();

    TokenCredentials creds = new TokenCredentials(jwtToken);

    jwtAuth
        .authenticate(creds)
        .onSuccess(
            user -> {

              JwtData jwtData = new JwtData(user.principal());
              jwtData.setExp(user.get("exp"));
              jwtData.setIat(user.get("iat"));
              LOGGER.debug("jwt: "+jwtData);
              promise.complete(jwtData);
            })
        .onFailure(
            err -> {
              LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
              promise.fail("failed");
            });

    return promise.future();

  }

  public Future<JsonObject> validateAccess(
      JwtData jwtData, boolean openResource, JsonObject authInfo) {
    JsonObject Response = new JsonObject();
    LOGGER.trace("validateAccess() started");

    Promise<JsonObject> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];
    if (openResource && checkOpenEndPoints(authInfo.getString("apiEndpoint"))) {
      LOGGER.info("User access is allowed.");
      JsonObject jsonResponse = new JsonObject();
      jsonResponse.put(JSON_IID, jwtId);
      jsonResponse.put(JSON_USERID, jwtData.getSub());
      jsonResponse.put(ROLE, jwtData.getRole());
      jsonResponse.put(DRL, jwtData.getDrl());
      jsonResponse.put(DID, jwtData.getDid());

      JsonArray accessibleAttrs = jwtData.getCons().getJsonArray("attrs");
      if (accessibleAttrs == null || accessibleAttrs.isEmpty()) {
        jsonResponse.put(ACCESS, new JsonArray());
      } else {
        jsonResponse.put(ACCESS, accessibleAttrs);
      }
      jsonResponse.put(ACCESS, jwtData.getCons().getValue("access",new JsonArray()));



      return Future.succeededFuture(jsonResponse);
    }

    if (checkQueryEndPoints(authInfo.getString("apiEndpoint"))) {
      LOGGER.info("User access is allowed. [Query endpoints]");
      JsonObject jsonResponse = new JsonObject();
      jsonResponse.put(JSON_IID, jwtId);
      jsonResponse.put(JSON_USERID, jwtData.getSub());
      jsonResponse.put(ROLE, jwtData.getRole());
      jsonResponse.put(DRL, jwtData.getDrl());
      jsonResponse.put(DID, jwtData.getDid());
      JsonArray accessibleAttrs = jwtData.getCons().getJsonArray("attrs");
      if (accessibleAttrs == null || accessibleAttrs.isEmpty()) {
        jsonResponse.put(ACCESS, new JsonArray());
      } else {
        jsonResponse.put(ACCESS, accessibleAttrs);
      }

      return Future.succeededFuture(jsonResponse);
    }

    Method method = Method.valueOf(authInfo.getString("method"));
    String api = authInfo.getString("apiEndpoint");
    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);

    AuthorizationStrategy authStrategy =
        AuthorizationContextFactory.create(jwtData.getRole(), this.api);
    LOGGER.info("strategy : " + authStrategy.getClass().getSimpleName());
    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    LOGGER.info("endPoint : " + authInfo.getString("apiEndpoint"));


    if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
      JsonObject jsonResponse = new JsonObject();
      jsonResponse.put(JSON_USERID, jwtData.getSub());
      jsonResponse.put(ROLE, jwtData.getRole());
      jsonResponse.put(DRL, jwtData.getDrl());
      jsonResponse.put(DID, jwtData.getDid());
      JsonArray accessibleAttrs = jwtData.getCons().getJsonArray("attrs");
      if (accessibleAttrs == null || accessibleAttrs.isEmpty()) {
        jsonResponse.put(ACCESS, new JsonArray());
      } else {
        jsonResponse.put(ACCESS, accessibleAttrs);
      }

      promise.complete(jsonResponse);
    } else {
      LOGGER.info("failed");

      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  Future<Boolean> isValidAudienceValue(JwtData jwtData) {
    JsonObject jsonResponse= new JsonObject();
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
    LOGGER.info("id : {}, jwtID : {}", id, jwtId);
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
    cache.get(
        requestJson,
        handler -> {
          if (handler.succeeded()) {
            JsonObject responseJson = handler.result();
            String timestamp = responseJson.getString("value");
            LocalDateTime revokedAt = LocalDateTime.parse(timestamp);
            LocalDateTime jwtIssuedAt =
                LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(jwtData.getIat()), ZoneId.systemDefault());
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

  // class to contain intermeddiate data for token interospection
  final class ResultContainer {
    JwtData jwtData;
    boolean isOpen;
  }
}
