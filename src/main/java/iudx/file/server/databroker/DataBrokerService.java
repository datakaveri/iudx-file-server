package iudx.file.server.databroker;

import io.vertx.codegen.annotations.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
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
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return DataBrokerServiceVertxEBProxy which is a service proxy
   */
  @GenIgnore
  static DataBrokerService createProxy(Vertx vertx, String address) {
    return new DataBrokerServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  DataBrokerService consumeMessageFromQueue(Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DataBrokerService getInvalidationDataFromDB(Handler<AsyncResult<JsonObject>> handler);
}
