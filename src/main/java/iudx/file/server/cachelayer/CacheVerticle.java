package iudx.file.server.cachelayer;

import iudx.file.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;

import static iudx.file.server.common.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.file.server.common.Constants.DATA_BROKER_SERVICE_ADDRESS;

/**
 * The Cache Layer Verticle
 * <h1>Cache Layer Verticle</h1>
 * <p>
 *   The Cache Layer Verticle implementation in the IUDX File Server exposes the
 *   {@link iudx.file.server.cachelayer.CacheService} over the Vert.x Event Bus.
 * </p>
 *
 * @version 1.0
 * @since 2022-01-03
 */

public class CacheVerticle extends AbstractVerticle{

  private static final Logger LOGGER = LogManager.getLogger(CacheVerticle.class);
  private CacheService cacheService;
  private DataBrokerService dataBrokerService;

  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  @Override
  public void start() throws Exception {

    dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);

    cacheService = new CacheServiceImpl(vertx, dataBrokerService);

    binder = new ServiceBinder(vertx);

    consumer = binder
            .setAddress(CACHE_SERVICE_ADDRESS)
            .register(CacheService.class, cacheService);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
