package iudx.file.server.authenticator;

import static iudx.file.server.common.Constants.AUTH_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.serviceproxy.ServiceBinder;
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


  @Override
  public void start() {

    webClientFactory = new WebClientFactory(vertx, config());
    catalogueService = new CatalogueServiceImpl(vertx, webClientFactory, config());
   // auth = new AuthenticationServiceImpl(vertx, catalogueService, webClientFactory, config());
    
    JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
    jwtAuthOptions.addPubSecKey(
        new PubSecKeyOptions()
            .setAlgorithm("ES256")
            .setBuffer(
                "-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAErpLtzpUlszGvlbodGbCrfp3VlxIR\nfG/xkZiy5Jqtgryjv2IIj4vLwJbwm4jMP6WW8f5tAshbvoqYGCzTSBo8Rg==\n-----END PUBLIC KEY-----\n"));
    /* Default jwtIgnoreExpiry is false. If set through config, then that value is taken */
    boolean jwtIgnoreExpiry = config().getBoolean("jwtIgnoreExpiry") == null ? false
        : config().getBoolean("jwtIgnoreExpiry");
    if (jwtIgnoreExpiry) {
      jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
      //LOGGER.warn("JWT ignore expiration set to true, do not set IgnoreExpiration in production!!");
    }
    JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

    //@TODO: replace binder with jwt once auth server available.
    jwtAuthenticationService =
        new JwtAuthenticationServiceImpl(vertx, jwtAuth,  config(),catalogueService);
    

    binder = new ServiceBinder(vertx);

    consumer =
        binder.setAddress(authAddress)
            .register(AuthenticationService.class, jwtAuthenticationService);

  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
