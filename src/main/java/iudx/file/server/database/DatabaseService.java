package iudx.file.server.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface DatabaseService {


  @Fluent
  DatabaseService search(final JsonObject query, final Handler<AsyncResult<JsonObject>> handler);


  @Fluent
  DatabaseService save(final JsonObject json, final Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService delete(final String id, final Handler<AsyncResult<JsonObject>> handler);


  @GenIgnore
  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }
}
