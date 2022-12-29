package iudx.file.server.authenticator;

import static iudx.file.server.authenticator.authorization.Method.GET;
import static iudx.file.server.authenticator.authorization.Method.POST;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import iudx.file.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import io.micrometer.core.ipc.http.HttpSender.Method;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.authenticator.authorization.AuthorizationRequest;
import iudx.file.server.authenticator.utilities.JwtData;
import iudx.file.server.cache.CacheService;
import iudx.file.server.common.WebClientFactory;
import iudx.file.server.common.service.CatalogueService;
import iudx.file.server.common.service.impl.CatalogueServiceImpl;
import iudx.file.server.configuration.Configuration;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class JwtAuthServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthServiceTest.class);
  private static JsonObject authConfig;
  private static JwtAuthenticationServiceImpl jwtAuthenticationService;
  private static Configuration config;
  private static CatalogueService catalogueServiceMock;
  private static JwtAuthenticationServiceImpl jwtAuthImplSpy;
  private static WebClientFactory webClientFactory;
  private static CacheService cacheServiceMock;
  @Mock
  HttpRequest<Buffer> httpRequestMock;
  @Mock
  HttpResponse<Buffer> httpResponseMock;

  private static String delegateJwt =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJhMTNlYjk1NS1jNjkxLTRmZDMtYjIwMC1mMThiYzc4ODEwYjUiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2MjgxODIzMjcsImlhdCI6MTYyODEzOTEyNywiaWlkIjoicmk6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cC9yZXNvdXJjZSIsInJvbGUiOiJkZWxlZ2F0ZSIsImNvbnMiOnsiYWNjZXNzIjpbImFwaSIsInN1YnMiLCJpbmdlc3QiLCJmaWxlIl19fQ.tUoO1L-tXByxNtjY_iK41neeshCiYrNr505wWn1hC1ACwoeL9frebABeFiCqJQGrsBsGOZ1-OACZdHBNcetwyw";
  private static String consumerJwt =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIzMmE0Yjk3OS00ZjRhLTRjNDQtYjBjMy0yZmUxMDk5NTJiNWYiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2MjgxODUzNTksImlhdCI6MTYyODE0MjE1OSwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJjb25zdW1lciIsImNvbnMiOnsiYWNjZXNzIjpbImFwaSIsInN1YnMiLCJpbmdlc3QiLCJmaWxlIl19fQ.NoEiJB_5zwTU-zKbFHTefMuqDJ7L6mA11mfckzA4IZOSrdweSmR6my0zGcf7hEVljX9OOFm4tToZQYfCtPg4Uw";
  private static String providerJwt =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJhMTNlYjk1NS1jNjkxLTRmZDMtYjIwMC1mMThiYzc4ODEwYjUiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2MjgxODU4MjEsImlhdCI6MTYyODE0MjYyMSwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJwcm92aWRlciIsImNvbnMiOnsiYWNjZXNzIjpbImFwaSIsInN1YnMiLCJpbmdlc3QiLCJmaWxlIl19fQ.BSoCQPUT8_YA-6p7-_OEUBOfbbvQZs8VKwDzdnubT3gutVueRe42a9d9mhszhijMQK7Qa0ww_rmAaPhA_2jP6w";

  private static String closedResourceToken =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJhMTNlYjk1NS1jNjkxLTRmZDMtYjIwMC1mMThiYzc4ODEwYjUiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoicnMuaXVkeC5pbyIsImV4cCI6MTYyODYxMjg5MCwiaWF0IjoxNjI4NTY5NjkwLCJpaWQiOiJyZzppaXNjLmFjLmluLzg5YTM2MjczZDc3ZGFjNGNmMzgxMTRmY2ExYmJlNjQzOTI1NDdmODYvcnMuaXVkeC5pby9zdXJhdC1pdG1zLXJlYWx0aW1lLWluZm9ybWF0aW9uL3N1cmF0LWl0bXMtbGl2ZS1ldGEiLCJyb2xlIjoiY29uc3VtZXIiLCJjb25zIjp7ImFjY2VzcyI6WyJhcGkiLCJzdWJzIiwiaW5nZXN0IiwiZmlsZSJdfX0.OBJZUc15s8gDA6PB5IK3KkUGmjvJQWr7RvByhMXmmrCULmPGgtesFmNDVG2gqD4WXZob5OsjxZ1vxRmgMBgLxw";

  private static String openResourceToken =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiI4NDRlMjUxYi01NzRiLTQ2ZTYtOTI0Ny1mNzZmMWY3MGE2MzciLCJpc3MiOiJhdXRodmVydHguaXVkeC5pbyIsImF1ZCI6InJzLml1ZHguaW8iLCJleHAiOjE2MzEyMTQxNjgsImlhdCI6MTYzMTE3MDk2OCwiaWlkIjoicnM6cnMuaXVkeC5pbyIsInJvbGUiOiJjb25zdW1lciIsImNvbnMiOnt9fQ.ATXT7FUkuWiEkfQECW4kIjuiGmUbYh51k-8as5-XLUXrWJVFI6LaJnk2JE6gr_RKknNksGEuxodO2rGzkEhfLQ";

  private String id =
      "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";

  private static String dxApiBasePath;
  private static String iudxApiBasePath;
  private static Api api;

  @BeforeAll
  @DisplayName("Initialize Vertx and deploy Auth Verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {
    config = new Configuration();
    authConfig = config.configLoader(1, vertx);
    authConfig.put("host", "rs.iudx.io");
    authConfig.put("dxApiBasePath","/ngsi-ld/v1");
    authConfig.put("iudxApiBasePath","/iudx/v1");



    JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
    jwtAuthOptions.addPubSecKey(
        new PubSecKeyOptions()
            .setAlgorithm("ES256")
            .setBuffer("-----BEGIN PUBLIC KEY-----\n" +
                "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8BKf2HZ3wt6wNf30SIsbyjYPkkTS\n" +
                "GGyyM2/MGF/zYTZV9Z28hHwvZgSfnbsrF36BBKnWszlOYW0AieyAUKaKdg==\n" +
                "-----END PUBLIC KEY-----\n" +
                ""));
    jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);// ignore token expiration only for
                                                             // test
    JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

    webClientFactory = new WebClientFactory(vertx, authConfig);
    // catalogueService = new CatalogueServiceImpl(vertx, webClientFactory, authConfig);
    catalogueServiceMock = mock(CatalogueServiceImpl.class);
    cacheServiceMock = mock(CacheService.class);
    dxApiBasePath = authConfig.getString("dxApiBasePath");
    iudxApiBasePath = authConfig.getString("iudxApiBasePath");
    api = Api.getInstance(dxApiBasePath,iudxApiBasePath);
    jwtAuthenticationService = new JwtAuthenticationServiceImpl(vertx, jwtAuth, authConfig,
        catalogueServiceMock, cacheServiceMock, api);
    jwtAuthImplSpy = spy(jwtAuthenticationService);

    LOGGER.info("Auth tests setup complete");
    testContext.completeNow();
  }



  @Test
  @DisplayName("decode valid jwt")
  public void decodeJwtProviderSuccess(VertxTestContext testContext) {
    jwtAuthenticationService.decodeJwt(providerJwt).onComplete(handler -> {
      if (handler.succeeded()) {
        assertEquals("provider", handler.result().getRole());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("decode valid jwt - delegate")
  public void decodeJwtDelegateSuccess(VertxTestContext testContext) {
    jwtAuthenticationService.decodeJwt(delegateJwt).onComplete(handler -> {
      if (handler.succeeded()) {
        assertEquals("delegate", handler.result().getRole());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("decode valid jwt - consumer")
  public void decodeJwtConsumerSuccess(VertxTestContext testContext) {
    jwtAuthenticationService.decodeJwt(consumerJwt).onComplete(handler -> {
      if (handler.succeeded()) {
        assertEquals("consumer", handler.result().getRole());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("decode invalid jwt")
  public void decodeJwtFailure(VertxTestContext testContext) {
    String jwt =
        "eyJ0eXAiOiJKV1QiLCJbGciOiJFUzI1NiJ9.eyJzdWIiOiJhM2U3ZTM0Yy00NGJmLTQxZmYtYWQ4Ni0yZWUwNGE5NTQ0MTgiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2Mjc2ODk5NDAsImlhdCI6MTYyNzY0Njc0MCwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJkZWxlZ2F0ZSIsImNvbnMiOnt9fQ.eJjCUvWuGD3L3Dn2fKj8Ydl1byGoyRS59VfL6ZJcdKR3_eIhm6SOY-CW3p5XDSYVhRTlWvlPLjfXYo9t_PxgnA";
    jwtAuthenticationService.decodeJwt(jwt).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow(handler.cause());
      } else {
        testContext.completeNow();

      }
    });
  }

  @Test
  @DisplayName("success - allow consumer access to /entities endpoint for access [api,subs]")
  public void access4ConsumerTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    String id =
        "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";

    authInfo.put("token", consumerJwt);
    authInfo.put("id", id);
    authInfo.put("apiEndpoint", api.getApiFileDownload());
    authInfo.put("method", Method.GET);

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("file.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid(
        "ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("file")));


    jwtAuthenticationService.validateAccess(jwtData, true, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }

  //@Disabled
  @Test
  @DisplayName("success - allow  access to query endpoint for open token")
  public void access4QueryAPIOpenToken(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    String id =
        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta";

    authInfo.put("token", openResourceToken);
    authInfo.put("id", id);
    authInfo.put("apiEndpoint", api.getApiTemporal());
    authInfo.put("method", Method.GET);

    JsonObject request = new JsonObject();

    doAnswer(Answer -> Future.succeededFuture(true)).when(catalogueServiceMock).isItemExist(any());
    doAnswer(Answer -> Future.succeededFuture(true)).when(jwtAuthImplSpy)
        .isValidAudienceValue(any());
    doAnswer(Answer -> Future.succeededFuture("CLOSE")).when(jwtAuthImplSpy).isOpenResource(any());


    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(false);


    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(cacheServiceMock).get(any(), any());

    jwtAuthImplSpy.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("failed");
      }
    });
  }
  
 
  @Test
  @DisplayName("success - allow  access to query endpoint for close token")
  public void access4QueryAPICloseToken(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    String id =
        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta";

    authInfo.put("token", closedResourceToken);
    authInfo.put("id", id);
    authInfo.put("apiEndpoint", api.getApiTemporal());
    authInfo.put("method", Method.GET);

    JsonObject request = new JsonObject();

    doAnswer(Answer -> Future.succeededFuture(true)).when(catalogueServiceMock).isItemExist(any());
    doAnswer(Answer -> Future.succeededFuture(true)).when(jwtAuthImplSpy)
        .isValidAudienceValue(any());
    doAnswer(Answer -> Future.succeededFuture("CLOSE")).when(jwtAuthImplSpy).isOpenResource(any());


    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(false);


    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(cacheServiceMock).get(any(), any());

    jwtAuthImplSpy.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("failed");
      }
    });
  }

  //@Disabled  
  @Test
  @DisplayName("success - allow  access to query open endpoint for open token")
  public void access4QueryOpenTokenOpenResource(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    String id =
        "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";

    authInfo.put("token", openResourceToken);
    authInfo.put("id", id);
    authInfo.put("apiEndpoint", api.getApiTemporal());
    authInfo.put("method", Method.GET);

    JsonObject request = new JsonObject();

    doAnswer(Answer -> Future.succeededFuture(true)).when(catalogueServiceMock).isItemExist(any());
    doAnswer(Answer -> Future.succeededFuture(true)).when(jwtAuthImplSpy)
        .isValidAudienceValue(any());
    doAnswer(Answer -> Future.succeededFuture("OPEN")).when(jwtAuthImplSpy).isOpenResource(any());


    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(false);


    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(cacheServiceMock).get(any(), any());

    jwtAuthImplSpy.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("failed");
      }
    });
  }


  @Test
  @DisplayName("fail - allow consumer access to /entities endpoint for null access")
  public void failAccess4ConsumerTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", consumerJwt);
    authInfo.put("id",
        "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", api.getApiFileDownload());
    authInfo.put("method", Method.GET);

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("file.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid(
        "ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(null);


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("success for invalid access");
      } else {
        testContext.completeNow();

      }
    });
  }

  @Test
  @DisplayName("success - allow delegate access to /entities endpoint")
  public void access4DelegateTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", consumerJwt);
    authInfo.put("id",
        "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", api.getApiFileDownload());
    authInfo.put("method", Method.GET);

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("file.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid(
        "ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("delegate");
    jwtData.setCons(null);


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }



  @Test
  @DisplayName("fail - consumer access to /entities endpoint for access [subs]")
  public void fail4ConsumerTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", consumerJwt);
    authInfo.put("id",
        "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", api.getApiFileDownload());
    authInfo.put("method", Method.GET);

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("file.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid(
        "ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("invalid access allowed");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("success - allow provider access to all")
  public void access4ProviderTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", consumerJwt);
    authInfo.put("id",
        "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", api.getApiFileDownload());
    authInfo.put("method", Method.GET);

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("file.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid(
        "ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("file")));


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }

  @Test
  @DisplayName("success - token interospection allow access")
  public void successTokenInterospect(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", consumerJwt);
    authInfo.put("id",
        "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", api.getApiFileDownload());
    authInfo.put("method", Method.GET);

    JsonObject request = new JsonObject();

    doAnswer(Answer -> Future.succeededFuture(true)).when(catalogueServiceMock).isItemExist(any());
    doAnswer(Answer -> Future.succeededFuture(true)).when(jwtAuthImplSpy)
        .isValidAudienceValue(any());
    doAnswer(Answer -> Future.succeededFuture("OPEN")).when(jwtAuthImplSpy).isOpenResource(any());


    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(false);


    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(cacheServiceMock).get(any(), any());

    jwtAuthImplSpy.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("failed");
      }
    });
  }

  @Test
  @DisplayName("success - token interospection deny access[invalid client id]")
  public void failureTokenInterospectRevokedClient(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", consumerJwt);
    authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    authInfo.put("apiEndpoint", api.getApiFileDownload());
    authInfo.put("method", Method.GET);

    JsonObject request = new JsonObject();

    doAnswer(Answer -> Future.succeededFuture(true)).when(catalogueServiceMock).isItemExist(any());
    doAnswer(Answer -> Future.succeededFuture(true)).when(jwtAuthImplSpy)
        .isValidAudienceValue(any());

    JsonObject cacheresponse = new JsonObject();
    JsonArray responseArray = new JsonArray();
    cacheresponse.put("value", "2019-10-19T14:20:00");
    responseArray.add(cacheresponse);


    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(new JsonObject().put("result", responseArray));


    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(cacheServiceMock).get(any(), any());

    jwtAuthImplSpy.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("failed");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("fail - token interospection deny access [invalid audience]")
  public void failureTokenInterospectInvalidAudience(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", consumerJwt);
    authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    authInfo.put("apiEndpoint", api.getApiFileDownload());
    authInfo.put("method", Method.GET);

    JsonObject request = new JsonObject();

    doAnswer(Answer -> Future.succeededFuture(true)).when(catalogueServiceMock).isItemExist(any());

    doAnswer(Answer -> Future.failedFuture("invalid audience value"))
        .when(jwtAuthImplSpy)
        .isValidAudienceValue(any());

    jwtAuthImplSpy.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("failed");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("fail - token interospection deny access [invalid resource]")
  public void failureTokenInterospectInvalidResource(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", consumerJwt);
    authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    authInfo.put("apiEndpoint", api.getApiFileDownload());
    authInfo.put("method", Method.GET);

    JsonObject request = new JsonObject();

    doAnswer(Answer -> Future.failedFuture("resource doesn't exist"))
        .when(catalogueServiceMock)
        .isItemExist(any());

    doAnswer(Answer -> Future.succeededFuture(true))
        .when(jwtAuthImplSpy)
        .isValidAudienceValue(any());

    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(false);


    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(cacheServiceMock).get(any(), any());

    jwtAuthImplSpy.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("failed");
      } else {
        testContext.completeNow();
      }
    });
  }


  @Test
  @DisplayName("fail - invalid audience value")
  public void fail4InvalidAud(VertxTestContext testContext) {

    JwtData jwt = new JwtData();
    jwt.setAud("rs.iudx.in");
    jwtAuthenticationService.isValidAudienceValue(jwt).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("passed for invalid value");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("success - valid audience value")
  public void success4ValidAud(VertxTestContext testContext) {

    JwtData jwt = new JwtData();
    jwt.setAud("rs.iudx.io");
    jwtAuthenticationService.isValidAudienceValue(jwt).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("failed for valid value");
      }
    });
  }


  @Test
  @DisplayName("fail - invalid id")
  public void fail4InvalidId(VertxTestContext testContext) {

    JwtData jwt = new JwtData();
    jwt.setIid("rg:abc/xyz");
    jwtAuthenticationService.isValidId(jwt, "abc/xyz/wxy").onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("passed for invalid id");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("success - valid id")
  public void success4validId(VertxTestContext testContext) {

    JwtData jwt = new JwtData();
    jwt.setIid("rg:abc/xyz");
    jwtAuthenticationService.isValidId(jwt, "abc/xyz").onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("failed for valid id");
      }
    });
  }

  @Test
  @DisplayName("success - is open resource")
  public void success4openResource(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();

    String id =
        "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";

    authInfo.put("token", consumerJwt);
    authInfo.put("id", id);
    authInfo.put("apiEndpoint", api.getApiFileDownload());
    authInfo.put("method", Method.GET);

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("file.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid(
        "ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("file")));

    doAnswer(Answer -> Future.succeededFuture("OPEN")).when(jwtAuthImplSpy).isOpenResource(any());

    jwtAuthImplSpy.isOpenResource(id).onComplete(openResourceHandler -> {
      if (openResourceHandler.succeeded() && openResourceHandler.result().equals("OPEN")) {
        testContext.completeNow();
      } else {
        testContext.failNow("open resource validation failed");
      }
    });
  }

  @Test
  @DisplayName("failure - is open resource")
  public void failure4openResource(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();

    String id = "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group";

    authInfo.put("token", consumerJwt);
    authInfo.put("id", id);
    authInfo.put("apiEndpoint", api.getApiFileDownload());
    authInfo.put("method", Method.GET);

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("file.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("ri:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("file")));

    jwtAuthenticationService.isOpenResource(id).onComplete(openResourceHandler -> {
      if (openResourceHandler.succeeded()) {
        testContext.failNow("open resource validation failed");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("authRequest should not equal")
  public void authRequestShouldNotEquals() {
    AuthorizationRequest authR1 = new AuthorizationRequest(POST, api.getApiFileUpload());
    AuthorizationRequest authR2 = new AuthorizationRequest(GET, api.getApiFileUpload());
    assertFalse(authR1.equals(authR2));
  }

  @Test
  @DisplayName("authRequest should have same hashcode")
  public void authRequestShouldhaveSamehash() {
    AuthorizationRequest authR1 = new AuthorizationRequest(POST, api.getApiFileUpload());
    AuthorizationRequest authR2 = new AuthorizationRequest(POST, api.getApiFileUpload());
    assertEquals(authR1.hashCode(), authR2.hashCode());
  }

  @Test
  @DisplayName("Test isOpenResource method for Cache miss for Valid Group ID")
  public void testIsOpenResource(VertxTestContext vertxTestContext) {

    AsyncResult<HttpResponse<Buffer>> asyncResultMock = mock(AsyncResult.class);

    JsonObject jsonObject2 = new JsonObject();
    jsonObject2.put("accessPolicy", "Dummy Access Policy");
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, jsonObject2);

    JsonObject jsonObject = new JsonObject();
    jsonObject.put("type", "urn:dx:cat:Success");
    jsonObject.put("totalHits", 3);
    jsonObject.put("results", jsonArray);

    jwtAuthenticationService.catWebClient = mock(WebClient.class);

    when(jwtAuthenticationService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequestMock);
    when(httpRequestMock.addQueryParam(anyString(), anyString())).thenReturn(httpRequestMock);
    when(httpRequestMock.expect(any())).thenReturn(httpRequestMock);
    when(asyncResultMock.result()).thenReturn(httpResponseMock);
    when(httpResponseMock.statusCode()).thenReturn(200);

    when(httpResponseMock.bodyAsJsonObject()).thenReturn(jsonObject);
    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResultMock);
        return null;
      }
    }).when(httpRequestMock).send(any());

    jwtAuthenticationService.isOpenResource(id).onComplete(openResourceHandler -> {
      if (openResourceHandler.succeeded()) {
        assertEquals("Dummy Access Policy", openResourceHandler.result());
        vertxTestContext.completeNow();
      } else {
        vertxTestContext
            .failNow("open resource validation failed : " + openResourceHandler.cause());

      }
    });
  }


  @Test
  @DisplayName("Test isOpenResource method for Cache miss with 0 total hits")
  public void testIsOpenResourceWith0TotalHits(VertxTestContext vertxTestContext) {

    AsyncResult<HttpResponse<Buffer>> asyncResultMock = mock(AsyncResult.class);

    JsonObject jsonObject2 = new JsonObject();
    jsonObject2.put("accessPolicy", "Dummy Access Policy");
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, jsonObject2);
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("type", "urn:dx:cat:Success");
    jsonObject.put("totalHits", 0);
    jsonObject.put("results", jsonArray);

    jwtAuthenticationService.catWebClient = mock(WebClient.class);

    when(jwtAuthenticationService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequestMock);
    when(httpRequestMock.addQueryParam(anyString(), anyString())).thenReturn(httpRequestMock);
    when(httpRequestMock.expect(any())).thenReturn(httpRequestMock);
    when(asyncResultMock.result()).thenReturn(httpResponseMock);
    when(httpResponseMock.statusCode()).thenReturn(200);
    when(httpResponseMock.bodyAsJsonObject()).thenReturn(jsonObject);

    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResultMock);
        return null;
      }
    }).when(httpRequestMock).send(any());

    jwtAuthenticationService.isOpenResource(id).onComplete(openResourceHandler -> {
      if (openResourceHandler.succeeded()) {
        vertxTestContext
            .failNow("open resource validation failed : " + openResourceHandler.cause());
      } else {
        assertNull(openResourceHandler.result());
        vertxTestContext.completeNow();
      }
    });
  }

}
