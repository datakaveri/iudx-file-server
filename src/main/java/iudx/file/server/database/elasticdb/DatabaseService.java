package iudx.file.server.database.elasticdb;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.file.server.common.QueryType;

@VertxGen
@ProxyGen
public interface DatabaseService {

  Future<JsonObject> search(final JsonObject query, final QueryType type);

  Future<JsonObject> save(final JsonObject json);

  Future<JsonObject> delete(final String id);

  @GenIgnore
  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }
}
