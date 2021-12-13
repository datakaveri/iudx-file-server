package iudx.file.server.apiserver.handlers;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.response.RestResponse;
import iudx.file.server.apiserver.utilities.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static iudx.file.server.apiserver.utilities.Constants.*;

public class ValidationFailureHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(ValidationFailureHandler.class);

  @Override
  public void handle(RoutingContext routingContext) {
    Throwable failure = routingContext.failure();

    if(failure instanceof DxRuntimeException) {
      DxRuntimeException exception = (DxRuntimeException) failure;
      LOGGER.error(exception.getUrn().getUrn() + " : " + exception.getMessage());
      HttpStatusCode code = HttpStatusCode.getByValue(exception.getStatusCode());

      JsonObject response = new RestResponse.Builder()
              .type(exception.getUrn().getUrn())
              .title(code.getDescription())
              .details(exception.getLocalizedMessage())
              .build()
              .toJson();

      routingContext.response()
              .putHeader(CONTENT_TYPE, APPLICATION_JSON)
              .setStatusCode(exception.getStatusCode())
              .end(response.toString());
    }

    if(failure instanceof RuntimeException) {
      RuntimeException exception = (RuntimeException) failure;
      LOGGER.error(exception.getMessage()+" ||| "+exception.getCause());
      routingContext.response()
              .putHeader(CONTENT_TYPE, APPLICATION_JSON)
              .setStatusCode(HttpStatus.SC_BAD_REQUEST)
              .end(validationFailureResponse().toString());
    }
    routingContext.next();
  }

  private JsonObject validationFailureResponse() {
    return new JsonObject()
            .put(JSON_TYPE, HttpStatusCode.BAD_REQUEST.getUrn())
            .put(JSON_TITLE, "Bad Request Data")
            .put(JSON_DETAIL, MSG_BAD_QUERY);
  }
}
