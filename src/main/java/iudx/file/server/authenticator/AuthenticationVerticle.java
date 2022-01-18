package iudx.file.server.authenticator;

import static iudx.file.server.common.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.file.server.cache.CacheService;
import iudx.file.server.common.WebClientFactory;
import iudx.file.server.common.service.CatalogueService;
import iudx.file.server.common.service.impl.CatalogueServiceImpl;

public class AuthenticationVerticle extends AbstractVerticle {

  private AuthenticationService auth;
  private CatalogueService catalogueService;
  private WebClientFactory webClientFactory;
  private static final String authAddress = AUTH_SERVICE_ADDRESS;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private AuthenticationService jwtAuthenticationService;
  private WebClient webClient;
  private CacheService cacheService;
  private static final Logger LOGGER = LogManager.getLogger(AuthenticationVerticle.class);

  @Override
  public void start() {

    webClientFactory = new WebClientFactory(vertx, config());
    catalogueService = new CatalogueServiceImpl(vertx, webClientFactory, config());
    getJwtPublicKey(vertx, config()).onSuccess(handler -> {
      String cert = handler;
      LOGGER.debug("cert : " + cert);
      binder = new ServiceBinder(vertx);
      JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
      jwtAuthOptions.addPubSecKey(new PubSecKeyOptions().setAlgorithm("ES256").setBuffer(cert));
      /*
       * Default jwtIgnoreExpiry is false. If set through config, then that value is taken
       */
      boolean jwtIgnoreExpiry = config().getBoolean("jwtIgnoreExpiry") == null ? false
          : config().getBoolean("jwtIgnoreExpiry");
      if (jwtIgnoreExpiry) {
        jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
        LOGGER
            .warn("JWT ignore expiration set to true, do not set IgnoreExpiration in production!!");
      }
      JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
      cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
      jwtAuthenticationService = new JwtAuthenticationServiceImpl(vertx, jwtAuth, config(),
          catalogueService, cacheService);
      /* Publish the Authentication service with the Event Bus against an address. */
      binder = new ServiceBinder(vertx);

      consumer = binder.setAddress(AUTH_SERVICE_ADDRESS).register(AuthenticationService.class,
          jwtAuthenticationService);
      LOGGER.info("AUTH service deployed");

    }).onFailure(handler -> {
      LOGGER.error("failed to get JWT public key from auth server");
      LOGGER.error("Authentication verticle deployment failed.");
    });
  }

  private Future<String> getJwtPublicKey(Vertx vertx, JsonObject config) {
    Promise<String> promise = Promise.promise();
    webClient = createWebClient(vertx, config);
    webClient.get(443, config.getString("authHost"), "/auth/v1/cert").send(handler -> {
      if (handler.succeeded()) {
        JsonObject json = handler.result().bodyAsJsonObject();
        promise.complete(json.getString("cert"));
        LOGGER.info("Fetched JWT public key from auth server");
      } else {
        promise.fail("fail to get JWT public key");
        LOGGER.info("failed to get JWT public key from auth server");
      }
    });
    return promise.future();
  }

  static WebClient createWebClient(Vertx vertx, JsonObject config) {
    return createWebClient(vertx, config, false);
  }

  public static WebClient createWebClient(Vertx vertxObj, JsonObject config, boolean testing) {
    WebClientOptions webClientOptions = new WebClientOptions();
    webClientOptions.setSsl(true).setTrustAll(true).setVerifyHost(false);
    return WebClient.create(vertxObj, webClientOptions);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
