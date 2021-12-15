package iudx.file.server.databroker;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * The Data Broker Service.
 * <h1>Data Broker Service</h1>
 * <p>
 *   The Data Broker Service in the IUDX File Server defines the operations to be performed with
 *   the IUDX Data Broker Server.
 * </p>
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2021-12-14
 */

@VertxGen
@ProxyGen
public interface DataBrokerService {

  /**
   * The getMessage implements the read from a queue of the data broker.
   *
   * @param queueName which is a String
   * @param handler which is a Request Handler
   */
  @Fluent
  DataBrokerService getMessage(String queueName, Handler<AsyncResult<JsonObject>> handler);
}
