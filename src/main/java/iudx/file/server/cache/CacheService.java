package iudx.file.server.cache;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface CacheService {
  @GenIgnore
  static CacheService createProxy(Vertx vertx, String address) {
    return new CacheServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  CacheService get(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  CacheService put(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  CacheService refresh(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

}
