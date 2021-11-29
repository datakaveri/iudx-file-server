package iudx.file.server.auditing.util;

import io.vertx.core.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.file.server.auditing.util.Constants.*;

public class QueryBuilder {
  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  private long getEpochTime(ZonedDateTime zst) {
    return zst.toInstant().toEpochMilli();
  }

  public JsonObject buildWriteQuery(JsonObject request) {

    if(!request.containsKey(API) ||!request.containsKey(USER_ID) || !request.containsKey(RESOURCE_ID) || !request.containsKey(PROVIDER_ID)) {
      return new JsonObject().put(ERROR,DATA_NOT_FOUND);
    }

    String primaryKey = UUID.randomUUID().toString().replace("-","");
    String userId = request.getString(USER_ID);
    String api = request.getString(API);
    String resourceID = request.getString(RESOURCE_ID);
    String providerID = request.getString(PROVIDER_ID);
    ZonedDateTime zst = ZonedDateTime.now();
    LOGGER.info("TIME ZST: " + zst);
    long time = getEpochTime(zst);

    StringBuilder query =
            new StringBuilder(
                    WRITE_QUERY
                            .replace("$1", primaryKey)
                            .replace("$2", userId)
                            .replace("$3", api)
                            .replace("$7", Long.toString(time))
                            .replace("$4", resourceID)
                            .replace("$5", providerID)
                            .replace("$6", zst.toString()));
    LOGGER.info("Info: Query: " + query);
    return new JsonObject().put(QUERY_KEY, query);
  }
}
