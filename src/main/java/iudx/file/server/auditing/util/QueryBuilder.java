package iudx.file.server.auditing.util;

import static iudx.file.server.auditing.util.Constants.API;
import static iudx.file.server.auditing.util.Constants.DATA_NOT_FOUND;
import static iudx.file.server.auditing.util.Constants.ERROR;
import static iudx.file.server.auditing.util.Constants.PROVIDER_ID;
import static iudx.file.server.auditing.util.Constants.QUERY_KEY;
import static iudx.file.server.auditing.util.Constants.RESOURCE_ID;
import static iudx.file.server.auditing.util.Constants.RESPONSE_SIZE;
import static iudx.file.server.auditing.util.Constants.USER_ID;
import static iudx.file.server.auditing.util.Constants.WRITE_QUERY;

import io.vertx.core.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {
  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  private long getEpochTime(ZonedDateTime zst) {
    return zst.toInstant().toEpochMilli();
  }

  public JsonObject buildWriteQuery(JsonObject request) {

    if (!request.containsKey(API)
        || !request.containsKey(USER_ID)
        || !request.containsKey(RESOURCE_ID)
        || !request.containsKey(PROVIDER_ID)) {
      return new JsonObject().put(ERROR, DATA_NOT_FOUND);
    }

    String primaryKey = UUID.randomUUID().toString().replace("-", "");
    String userId = request.getString(USER_ID);
    String api = request.getString(API);
    String resourceID = request.getString(RESOURCE_ID);
    String providerID = request.getString(PROVIDER_ID);
    ZonedDateTime zst = ZonedDateTime.now();
    long responseSize = request.getLong(RESPONSE_SIZE);
    LOGGER.debug("TIME ZST: " + zst);
    long time = getEpochTime(zst);
    String databaseTableName = request.getString("databaseTableName");

    StringBuilder query =
        new StringBuilder(
            WRITE_QUERY
                .replace("$0", databaseTableName)
                .replace("$1", primaryKey)
                .replace("$2", api)
                .replace("$3", userId)
                .replace("$4", Long.toString(time))
                .replace("$5", resourceID)
                .replace("$7", providerID)
                .replace("$6", zst.toString())
                .replace("$8", Long.toString(responseSize)));
    LOGGER.debug("Info: Query: " + query);
    return new JsonObject().put(QUERY_KEY, query);
  }
}
