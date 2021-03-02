package iudx.file.server.query;


import static iudx.file.server.utilities.Constants.COUNT;
import static iudx.file.server.utilities.Constants.DETAIL;
import static iudx.file.server.utilities.Constants.ERROR;
import static iudx.file.server.utilities.Constants.ERROR_TYPE;
import static iudx.file.server.utilities.Constants.FAILED;
import static iudx.file.server.utilities.Constants.INDEX_NOT_FOUND;
import static iudx.file.server.utilities.Constants.INVALID_RESOURCE_ID;
import static iudx.file.server.utilities.Constants.REASON;
import static iudx.file.server.utilities.Constants.RESULTS;
import static iudx.file.server.utilities.Constants.ROOT_CAUSE;
import static iudx.file.server.utilities.Constants.STATUS;
import static iudx.file.server.utilities.Constants.SUCCESS;
import static iudx.file.server.utilities.Constants.TITLE;
import static iudx.file.server.utilities.Constants.TYPE_KEY;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResponseBuilder {

  private String status;
  private JsonObject response;

  /** Initialise the object with Success or Failure. */

  ResponseBuilder(String status) {
    this.status = status;
    response = new JsonObject();
  }

  ResponseBuilder setTypeAndTitle(int statusCode) {
    response.put(ERROR_TYPE, statusCode);
    if (SUCCESS.equalsIgnoreCase(status)) {
      response.put(TITLE, SUCCESS);
    } else if (FAILED.equalsIgnoreCase(status)) {
      response.put(TITLE, FAILED);
    }
    return this;
  }

  /** Successful Database Request with responses > 0. */

  ResponseBuilder setMessage(JsonArray results) {
    response.put(RESULTS, results);
    return this;
  }

  /** Overloaded methods for Error messages. */

  ResponseBuilder setMessage(String error) {
    response.put(DETAIL, error);
    return this;
  }

  ResponseBuilder setMessage(JsonObject error) {
    int statusCode = error.getInteger(STATUS);
    String type = error.getJsonObject(ERROR.toLowerCase()).getString(TYPE_KEY);
    if (statusCode == 404 && INDEX_NOT_FOUND.equalsIgnoreCase(type)) {
      response.put(DETAIL, INVALID_RESOURCE_ID);
    } else {
      response.put(DETAIL,
          error.getJsonObject(ERROR.toLowerCase()).getJsonArray(ROOT_CAUSE).getJsonObject(0)
              .getString(REASON));
    }
    return this;
  }

  ResponseBuilder setCount(int count) {
    response.put(RESULTS, new JsonArray().add(new JsonObject().put(COUNT, count)));
    return this;
  }

  JsonObject getResponse() {
    return response;
  }
}
