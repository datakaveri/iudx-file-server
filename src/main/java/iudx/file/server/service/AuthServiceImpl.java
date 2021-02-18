package iudx.file.server.service;

import static iudx.file.server.utilities.Constants.*;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;

public class AuthServiceImpl implements AuthService {

  private static final Logger LOGGER = LogManager.getLogger(AuthServiceImpl.class);
  private final WebClient webClient;
  private final Vertx vertx;
  private final int catPort;
  private final String catHost;


  private final Cache<String, JsonObject> tipCache = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterAccess(CACHE_TIMEOUT, TimeUnit.MINUTES)
      .build();
  // resourceGroupCache will contains ACL info about all resource group in a resource server
  private final Cache<String, String> resourceGroupCache = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterAccess(CACHE_TIMEOUT, TimeUnit.MINUTES)
      .build();


  public AuthServiceImpl(Vertx vertx, WebClient webClient, JsonObject config) {
    this.vertx = vertx;
    this.webClient = webClient;
    this.catPort = config.getInteger("cataloguePort");
    this.catHost = config.getString("catalogueHost");
  }



  @Override
  public Future<JsonObject> tokenInterospect(JsonObject request, JsonObject authenticationInfo) {
    Promise<JsonObject> promise = Promise.promise();
    String token = authenticationInfo.getString("token");
    TokenInterospectionResultContainer responseContainer =
        new TokenInterospectionResultContainer();
    Future<JsonObject> tipResponseFut = retrieveTipResponse(token);
    tipResponseFut.compose(tipResponse -> {
      responseContainer.tipResponse = tipResponse;
      LOGGER.debug("Info: TIP Response is : " + tipResponse);
      String id = tipResponse.getJsonArray("request").getJsonObject(0).getString("id");
      return isItemExist(id);
    }).onSuccess(success -> {
      responseContainer.isItemExist = success;
      Future<JsonObject> isValid =
          validateAccess(responseContainer.tipResponse, success, authenticationInfo, request);
      isValid.onComplete(handler -> {
        if (handler.succeeded()) {
          promise.complete();
        } else {
          promise.fail(handler.cause().getMessage());
        }
      });
    });
    return promise.future();
  }



  private Future<JsonObject> retrieveTipResponse(String token) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject cacheResponse = tipCache.getIfPresent(token);
    if (cacheResponse == null) {
      LOGGER.debug("Cache miss calling auth server");
      // cache miss
      // call cat-server only when token not found in cache.
      JsonObject body = new JsonObject();
      body.put("token", token);
      webClient.post(443, "authHost", "/auth/v1/token/introspect")
          .expect(ResponsePredicate.JSON).sendJsonObject(body, httpResponseAsyncResult -> {
            if (httpResponseAsyncResult.failed()) {
              promise.fail(httpResponseAsyncResult.cause());
              return;
            }
            HttpResponse<Buffer> response = httpResponseAsyncResult.result();
            if (response.statusCode() != HttpStatus.SC_OK) {
              String errorMessage =
                  response.bodyAsJsonObject().getJsonObject("error").getString("message");
              promise.fail(new Throwable(errorMessage));
              return;
            }
            JsonObject responseBody = response.bodyAsJsonObject();
            String cacheExpiry = Instant.now(Clock.systemUTC())
                .plus(CACHE_TIMEOUT, ChronoUnit.MINUTES)
                .toString();
            responseBody.put("cache-expiry", cacheExpiry);
            tipCache.put(token, responseBody);
            promise.complete(responseBody);
          });
    } else {
      LOGGER.debug("Cache Hit");
      promise.complete(cacheResponse);
    }
    return promise.future();
  }

  private Future<Boolean> isItemExist(String id) {
    LOGGER.debug("isItemExist() started");
    Promise<Boolean> promise = Promise.promise();
    // String id = itemId.replace("/*", "");
    LOGGER.info("id : " + id);
    webClient.get(catPort, catHost, "/iudx/cat/v1/item").addQueryParam("id", id)
        .expect(ResponsePredicate.JSON).send(responseHandler -> {
          if (responseHandler.succeeded()) {
            HttpResponse<Buffer> response = responseHandler.result();
            JsonObject responseBody = response.bodyAsJsonObject();
            if (responseBody.getString("status").equalsIgnoreCase("success")
                && responseBody.getInteger("totalHits") > 0) {
              promise.complete(true);
            } else {
              promise.fail(responseHandler.cause());
            }
          } else {
            promise.fail(responseHandler.cause());
          }
        });
    return promise.future();
  }


  private Future<JsonObject> validateAccess(JsonObject tipResponse, boolean isExist,
      JsonObject authenticationInfo, JsonObject userRequest) {
    Promise<JsonObject> promise = Promise.promise();

    String allowedId = tipResponse.getJsonArray("request").getJsonObject(0).getString("id");
    String requestedId = userRequest.getJsonArray("ids").getString(0);
    List<String> allowedEndpoints = toList(tipResponse.getJsonArray("apis"));
    String endpoint = authenticationInfo.getString("apiEndpoint");

    if (isExist) {
      if (isAllowedId(allowedId, requestedId) && isAllowedEndpoint(allowedEndpoints, endpoint)) {
        promise.complete();
      } else {
        promise.fail("not allowed.");
      }
    } else {
      LOGGER.debug("id doesn't exist in Catalogue server");
      promise.fail("not found in catalogue.");
    }
    return promise.future();
  }

  public boolean isAllowedId(String allowedId, String requestedId) {
    String allowedGroupID = allowedId.substring(0, allowedId.lastIndexOf("/"));
    String requestedGroupID = requestedId.substring(0, requestedId.lastIndexOf("/"));
    return allowedId.equals(requestedId) || allowedGroupID.equals(requestedGroupID);
  }

  public boolean isAllowedEndpoint(List<String> allowedEndpoints, String endpoint) {
    return allowedEndpoints.contains(endpoint);
  }

  private class TokenInterospectionResultContainer {
    JsonObject tipResponse;
    HashMap<String, Boolean> catResponse;
    Boolean isItemExist;
  }

  private <T> List<T> toList(JsonArray arr) {
    if (arr == null) {
      return null;
    } else {
      return (List<T>) arr.getList();
    }
  }

}
