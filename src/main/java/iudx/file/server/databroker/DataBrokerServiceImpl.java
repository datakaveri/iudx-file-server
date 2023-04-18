package iudx.file.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import iudx.file.server.common.Response;

import static iudx.file.server.auditing.util.Constants.FAILED;
import static iudx.file.server.auditing.util.Constants.SUCCESS;

public class DataBrokerServiceImpl implements DataBrokerService {

  private RabbitMQClient client;

  public DataBrokerServiceImpl(RabbitMQClient client) {
    this.client = client;
  }

  /** This method will only publish messages to internal-communication exchanges. */
  @Override
  public DataBrokerService publishMessage(
      JsonObject request,
      String toExchange,
      String routingKey,
      Handler<AsyncResult<JsonObject>> handler) {

    Buffer buffer = Buffer.buffer(request.toString());

    if (!client.isConnected()) {
      client.start();
    }

    client.basicPublish(
        toExchange,
        routingKey,
        buffer,
        publishHandler -> {
          if (publishHandler.succeeded()) {
            JsonObject result = new JsonObject().put("type", SUCCESS);
            handler.handle(Future.succeededFuture(result));
          } else {
            Response respBuilder =
                new Response.Builder()
                    .withTitle(FAILED)
                    .withDetail(publishHandler.cause().getLocalizedMessage())
                    .build();
            handler.handle(Future.failedFuture(respBuilder.toString()));
          }
        });
    return this;
  }
}
