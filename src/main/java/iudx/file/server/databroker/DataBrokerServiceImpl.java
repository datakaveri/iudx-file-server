package iudx.file.server.databroker;

import io.vertx.core.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.core.buffer.Buffer;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQConsumer;

import java.util.concurrent.TimeUnit;

import static iudx.file.server.databroker.util.Constants.CACHE_TIMEOUT_AMOUNT;


/**
 * The Data Broker Service Implementation.
 * <h1>Data Broker Service Implementation</h1>
 * <p>
 * The Data Broker Service implementation in the IUDX File Server implements the definitions of
 * the {@link iudx.file.server.databroker.DataBrokerService}.
 * </p>
 *
 * @version 1.0
 * @since 2021-12-14
 */

public class DataBrokerServiceImpl implements DataBrokerService{

  private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);
  private final RabbitMQClient client;

  private final QueueOptions options =
          new QueueOptions()
                  .setMaxInternalQueueSize(1000)
                  .setKeepMostRecent(true);

  Cache<String, String> tokenInvalidationCache =
          CacheBuilder.newBuilder()
                  .maximumSize(100)
                  .expireAfterAccess(CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES)
                  .build();

  public DataBrokerServiceImpl(RabbitMQClient client) {
    this.client = client;
  }


  @Override
  public DataBrokerService getMessage(String queueName, Handler<AsyncResult<JsonObject>> handler) {

    if(!client.isConnected()) {
      client.start();
    }

    client.basicConsumer("file-server-token-invalidation", options, consumeHandler -> {
      if(consumeHandler.succeeded()) {
        RabbitMQConsumer rmqConsumer = consumeHandler.result();
        rmqConsumer.handler(message -> {
          Buffer body = message.body();
          if(body != null) {
            JsonObject result = new JsonObject(body);
            String userID = result.getString("sub");
            String timestamp = result.getString("time");
            tokenInvalidationCache.put(userID, timestamp);
            handler.handle(Future.succeededFuture(result));
          } else {
            LOGGER.error(consumeHandler.cause().getMessage());
            handler.handle(Future.failedFuture("null/empty message"));
          }
        });
      }
    });

    return this;
  }
}
