package iudx.file.server.database.elasticdb.elastic.exception;

import static iudx.file.server.auditing.util.Constants.DETAIL;
import static iudx.file.server.auditing.util.Constants.TITLE;
import static iudx.file.server.database.elasticdb.utilities.Constants.STATUS;
import static iudx.file.server.database.elasticdb.utilities.Constants.TYPE_KEY;

import io.vertx.core.json.JsonObject;
import iudx.file.server.apiserver.response.ResponseUrn;
import org.apache.http.HttpStatus;

public class ESQueryException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int statusCode;
  private final String message;
  ResponseUrn urn;

  public ESQueryException(final String message) {
    this(ResponseUrn.BAD_REQUEST_URN, message, HttpStatus.SC_BAD_REQUEST);
  }

  public ESQueryException(final ResponseUrn urn, final String message) {
    this(urn, message, HttpStatus.SC_BAD_REQUEST);
  }

  public ESQueryException(final ResponseUrn urn, final String message, final int statusCode) {
    super(message);
    this.urn = urn;
    this.message = message;
    this.statusCode = statusCode;
  }

  public String toString() {
    JsonObject json = new JsonObject();
    json.put(STATUS, statusCode);
    json.put(TYPE_KEY, urn.getUrn());
    json.put(TITLE, urn.getMessage());
    json.put(DETAIL, message);
    return json.toString();
  }
}
