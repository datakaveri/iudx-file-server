package iudx.file.server.authenticator;

import static iudx.file.server.authenticator.utilities.Constants.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
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


  @Override
  public void start() {

    webClientFactory = new WebClientFactory(vertx, config());
    catalogueService = new CatalogueServiceImpl(vertx, webClientFactory, config());
    auth = new AuthenticationServiceImpl(vertx, catalogueService, webClientFactory, config());

    binder = new ServiceBinder(vertx);

    consumer =
        binder.setAddress(authAddress)
            .register(AuthenticationService.class, auth);

  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
