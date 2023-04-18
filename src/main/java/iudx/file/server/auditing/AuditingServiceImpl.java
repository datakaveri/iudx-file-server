package iudx.file.server.auditing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.file.server.auditing.util.QueryBuilder;
import iudx.file.server.common.Response;
import iudx.file.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.file.server.auditing.util.Constants.EXCHANGE_NAME;
import static iudx.file.server.auditing.util.Constants.ROUTING_KEY;
import static iudx.file.server.common.Constants.DATABROKER_SERVICE_ADDRESS;

public class AuditingServiceImpl implements AuditingService {

  private static final Logger LOGGER = LogManager.getLogger(AuditingServiceImpl.class);
  public static DataBrokerService rmqService;
  private final QueryBuilder queryBuilder = new QueryBuilder();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private JsonObject writeMessage = new JsonObject();

  public AuditingServiceImpl(Vertx vertxInstance) {
    this.rmqService = DataBrokerService.createProxy(vertxInstance, DATABROKER_SERVICE_ADDRESS);
  }

  @Override
  public AuditingService executeWriteQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    writeMessage = queryBuilder.buildWriteQueryForRmq(request);

    rmqService.publishMessage(
        writeMessage,
        EXCHANGE_NAME,
        ROUTING_KEY,
        rmqHandler -> {
          if (rmqHandler.succeeded()) {
            handler.handle(Future.succeededFuture());
            LOGGER.info("inserted into rmq");
          } else {
            LOGGER.error(rmqHandler.cause());
            try {
              Response resp =
                  objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
              LOGGER.debug("response from rmq " + resp);
              handler.handle(Future.failedFuture(resp.toString()));
            } catch (JsonProcessingException e) {
              LOGGER.error("Failure message not in format [type,title,detail]");
              handler.handle(Future.failedFuture(e.getMessage()));
            }
          }
        });
    return this;
  }
}
