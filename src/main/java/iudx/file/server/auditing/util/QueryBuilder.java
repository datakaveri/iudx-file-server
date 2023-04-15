package iudx.file.server.auditing.util;

import io.vertx.core.json.JsonObject;

import java.util.UUID;

import static iudx.file.server.auditing.util.Constants.ORIGIN;
import static iudx.file.server.auditing.util.Constants.PRIMARY_KEY;
import static iudx.file.server.auditing.util.Constants.ORIGIN_SERVER;

public class QueryBuilder {
  public JsonObject buildWriteQueryForRMQ(JsonObject request) {

    String primaryKey = UUID.randomUUID().toString().replace("-", "");

    request.put(PRIMARY_KEY, primaryKey);

    request.put(ORIGIN, ORIGIN_SERVER);

    return request;
  }
}
