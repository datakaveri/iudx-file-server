package iudx.file.server.auditing.util;

import static iudx.file.server.auditing.util.Constants.*;

import io.vertx.core.json.JsonObject;
import java.util.UUID;

/**
 * QueryBuilder.
 *
 * <h1>QueryBuilder</h1>
 */
public class QueryBuilder {
  /**
   * buildWriteQueryForRmq.
   *
   * @param request json data
   */
  public JsonObject buildWriteQueryForRmq(JsonObject request) {

    String primaryKey = UUID.randomUUID().toString().replace("-", "");

    request.put(PRIMARY_KEY, primaryKey);

    request.put(ORIGIN, ORIGIN_SERVER);

    return request;
  }
}
