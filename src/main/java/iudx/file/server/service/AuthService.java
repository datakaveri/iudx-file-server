package iudx.file.server.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface AuthService {

  public Future<JsonObject> tokenInterospect(JsonObject request, JsonObject authenticationInfo);
}
