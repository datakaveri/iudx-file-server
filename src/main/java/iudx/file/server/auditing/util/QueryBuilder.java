package iudx.file.server.auditing.util;

import io.vertx.core.json.JsonObject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
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

    if(!request.containsKey(API) ||!request.containsKey(USER_ID) || !request.containsKey(RESOURCE_ID)) {
      return new JsonObject().put(ERROR,DATA_NOT_FOUND);
    }

    String primaryKey = UUID.randomUUID().toString().replace("-","");
    String userId = request.getString(USER_ID);
    String api = request.getString(API);
    String resourceID = request.getString(RESOURCE_ID);
    ZonedDateTime zst = ZonedDateTime.now();
    LOGGER.info("TIME ZST: " + zst);
    long time = getEpochTime(zst);

    StringBuilder query =
            new StringBuilder(
                    WRITE_QUERY
                            .replace("$1", primaryKey)
                            .replace("$2", userId)
                            .replace("$3", api)
                            .replace("$6", Long.toString(time))
                            .replace("$4", resourceID)
                            .replace("$5", zst.toString()));
    LOGGER.info("Info: Query " + query);
    return new JsonObject().put(QUERY_KEY, query);
  }

  public JsonObject buildReadQuery(JsonObject request) {

    ZonedDateTime startZDT, endZDT;
    String userId, api;
    long fromTime = 0;
    long toTime = 0;

    if(!request.containsKey(USER_ID)) {
      return new JsonObject().put(ERROR, USERID_NOT_FOUND);
    }
    userId = request.getString(USER_ID);

    if(request.containsKey(START_TIME)) {
      try {
        startZDT = ZonedDateTime.parse(request.getString(START_TIME));
        LOGGER.debug("Parsed date-time: "+startZDT.toString());
      } catch (DateTimeParseException e) {
        LOGGER.error("Invalid date-time exception: "+e.getMessage());
        return new JsonObject().put(ERROR, INVALID_DATE_TIME);
      }

      if(request.containsKey(END_TIME)) {
        try {
          endZDT = ZonedDateTime.parse(request.getString(END_TIME));
          LOGGER.debug("Parsed date-time: "+endZDT.toString());
        } catch (DateTimeParseException e) {
          LOGGER.error("Invalid date-time exception: "+e.getMessage());
          return new JsonObject().put(ERROR, INVALID_DATE_TIME);
        }
      } else {
        return new JsonObject().put(ERROR, MISSING_END_TIME);
      }

      if(startZDT.isAfter(endZDT)) {
        LOGGER.error("Invalid date-time exception: ");
        return new JsonObject().put(ERROR, INVALID_TIME);
      }

      fromTime = getEpochTime(startZDT);
      toTime = getEpochTime(endZDT);
      LOGGER.debug("Epoch fromTime: " + fromTime);
      LOGGER.debug("Epoch toTime: " + toTime);

    } else {
      if(request.containsKey(END_TIME)) {
        return new JsonObject().put(ERROR, MISSING_START_TIME);
      }
    }

    StringBuilder userIdQuery = new StringBuilder(READ_QUERY.replace("$1", userId));
    LOGGER.info("Info: QUERY: " + userIdQuery);

    if (request.containsKey(START_TIME) && request.containsKey(END_TIME)) {
      userIdQuery.append(START_TIME_QUERY.replace("$2", Long.toString(fromTime)));
      userIdQuery.append(END_TIME_QUERY.replace("$3", Long.toString(toTime)));
      LOGGER.info("Info: QUERY with start and end time: " + userIdQuery);
    }
    if (request.containsKey(API)) {
      api = request.getString(API);
      userIdQuery.append(API_QUERY.replace("$4", api));
      LOGGER.info("Info: QUERY with endpoint: " + userIdQuery);
      return new JsonObject().put(QUERY_KEY, userIdQuery);
    }
    return new JsonObject().put(QUERY_KEY, userIdQuery);
  }
}
