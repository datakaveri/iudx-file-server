package iudx.file.server.auditing.util;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static iudx.file.server.auditing.util.Constants.*;

public class QueryBuilder {
  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  public JsonObject buildWriteQueryForRMQ(JsonObject request) {

    String primaryKey = UUID.randomUUID().toString().replace("-", "");

    request.put(PRIMARY_KEY, primaryKey);

    request.put(ORIGIN, ORIGIN_SERVER);

    return request;
  }
}
