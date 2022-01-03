package iudx.file.server.cachelayer;

import io.vertx.codegen.annotations.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Cache Service.
 * <h1>Cache Service</h1>
 * <p>
 *   The Cache Service in the IUDX File Server defines the operations to be performed with
 *   the IUDX Cache Layer.
 * </p>
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2022-01-03
 */

@VertxGen
@ProxyGen
public interface CacheService {

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return CacheServiceVertxEBProxy which is a service proxy
   */
  @GenIgnore
  static CacheService createProxy(Vertx vertx, String address) {
    return new CacheServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  CacheService populateCache(JsonObject jsonObject);

  @Fluent
  CacheService getInvalidationTimeFromCache(String id, Handler<AsyncResult<String>> handler);

}
