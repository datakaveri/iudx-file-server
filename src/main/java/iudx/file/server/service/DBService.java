package iudx.file.server.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface DBService {

  public void query(final JsonObject query, final Handler<AsyncResult<JsonObject>> handler);
  
  
  public void insert(final JsonObject value,final Handler<AsyncResult<JsonObject>> handler);

}
