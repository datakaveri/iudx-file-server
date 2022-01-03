package iudx.file.server.databroker;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.file.server.cachelayer.CacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.QueueOptions;


import static iudx.file.server.common.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.file.server.databroker.util.Constants.*;

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
  private final PostgresClient pgClient;
  private final CacheService cacheService;

  private final QueueOptions options =
          new QueueOptions()
                  .setMaxInternalQueueSize(1000)
                  .setKeepMostRecent(true);


  public DataBrokerServiceImpl(RabbitMQClient client, PostgresClient pgClient, Vertx vertx) {
    this.client = client;
    this.pgClient = pgClient;
    this.client.start(startHandler -> {
      if(startHandler.succeeded()) {
        LOGGER.info("RMQ started");
      } else {
        LOGGER.error("RMQ start failed");
      }
    });

    cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);

    /* consume message from queue on startup */
    consumeMessageFromQueue(handler -> {
      if(handler.succeeded()) {
        LOGGER.info("Queue messages consumed ");
      }
    });
  }

  @Override
  public DataBrokerService consumeMessageFromQueue(Handler<AsyncResult<JsonObject>> handler) {

    if (!client.isConnected()) {
      client.start();
    }

    client.basicConsumer(QUEUE_NAME, options, consumeHandler -> {
      if(consumeHandler.succeeded()) {
        getInvalidationDataFromDB(invalidationHandler -> {
          if(invalidationHandler.succeeded()) {
            cacheService.populateCache(invalidationHandler.result());
            handler.handle(Future.succeededFuture());
          } else {
            handler.handle(Future.failedFuture(invalidationHandler.cause()));
          }
        });
      } else {
        LOGGER.info(consumeHandler.cause().getMessage());
        handler.handle(Future.failedFuture("null/empty message"));
      }
    });

    return this;
  }


  @Override
  public DataBrokerService getInvalidationDataFromDB(Handler<AsyncResult<JsonObject>> resultHandler) {

    pgClient.getAsync(QUERY).onComplete(dbHandler -> {
      if(dbHandler.succeeded()) {
        RowSet<Row> rowSet = dbHandler.result();
        JsonObject invalidationResult =  new JsonObject();
        for(Row rs : rowSet) {
          String userID = String.valueOf(rs.getUUID("_id"));
          String timestamp = String.valueOf(rs.getLocalDateTime("modified_at"));
          invalidationResult.put(userID,timestamp);
        }
        resultHandler.handle(Future.succeededFuture(invalidationResult));
      } else {
        LOGGER.fatal("Fail: Read from DB failed");
        resultHandler.handle(Future.failedFuture(dbHandler.cause()));
      }
    });

    return this;
  }
}
