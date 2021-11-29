package iudx.file.server.auditing;

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
public interface AuditingService {

  @GenIgnore
  static AuditingService createProxy(Vertx vertx, String address) {
    return new AuditingServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  AuditingService executeWriteQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);
}
