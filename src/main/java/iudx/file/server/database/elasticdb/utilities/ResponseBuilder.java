package iudx.file.server.database.elasticdb.utilities;


import static iudx.file.server.database.elasticdb.utilities.Constants.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.file.server.apiserver.utilities.HttpStatusCode;

public class ResponseBuilder {


  private JsonObject response;

  /** Initialise the object with Success or Failure. */

  public ResponseBuilder() {
    response = new JsonObject();
  }

  public ResponseBuilder setTypeAndTitle(int statusCode) {
    response.put(ERROR_TYPE, HttpStatusCode.getByValue(statusCode).getValue());
    response.put(TITLE, HttpStatusCode.getByValue(statusCode).getUrn());

    return this;
  }

  /** Successful Database Request with responses > 0. */

  public ResponseBuilder setMessage(JsonArray results) {
    response.put(RESULTS, results);
    return this;
  }

  /** Overloaded methods for Error messages. */

  public ResponseBuilder setMessage(String error) {
    response.put(DETAIL, error);
    return this;
  }

  public ResponseBuilder setMessage(JsonObject error) {
    int statusCode = error.getInteger(STATUS);
    String type = error.getJsonObject(ERROR.toLowerCase()).getString(TYPE_KEY);
    if (statusCode == 404 && INDEX_NOT_FOUND.equalsIgnoreCase(type)) {
      response.put(ERROR_MESSAGE, INVALID_RESOURCE_ID);
    } else {
      response.put(ERROR_MESSAGE,
          error.getJsonObject(ERROR.toLowerCase()).getJsonArray(ROOT_CAUSE).getJsonObject(0)
              .getString(REASON));
    }
    return this;
  }

  public ResponseBuilder setCount(long count) {
    response.put(RESULTS, new JsonArray().add(new JsonObject().put(COUNT, count)));
    return this;
  }
  
//  public ResponseBuilder setFromParam(int from) {
//    response.put(PARAM_OFFSET, from);
//    return this;
//  }
//  
//  public ResponseBuilder setSizeParam(int size) {
//    response.put(PARAM_LIMIT, size);
//    return this;
//  }
  
  public ResponseBuilder setTotalDocsCount(int totalDocs) {
   response.put("totalHits", totalDocs);
   return this;
  }

  public JsonObject getResponse() {
    return response;
  }
}
