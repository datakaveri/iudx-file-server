package iudx.file.server.apiserver.handlers;

import static iudx.file.server.apiserver.utilities.Constants.APPLICATION_JSON;
import static iudx.file.server.apiserver.utilities.Constants.CONTENT_TYPE;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.file.server.apiserver.validations.RequestType;
import iudx.file.server.apiserver.validations.ValidationHandlerFactory;
import iudx.file.server.apiserver.validations.types.Validator;

public class ValidationsHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(ValidationsHandler.class);

  private RequestType requestType;


  public ValidationsHandler(RequestType apiRequestType) {
    this.requestType = apiRequestType;
  }

  @Override
  public void handle(RoutingContext context) {
    ValidationHandlerFactory validationFactory = new ValidationHandlerFactory();
    MultiMap parameters = context.request().params();
    MultiMap headers = context.request().headers();
    List<Validator> validations = null;
    
    validations=validationFactory.create(requestType, parameters, headers);
    
    for (Validator validator : Optional.ofNullable(validations).orElse(Collections.emptyList())) {
      LOGGER.debug("validator :" + validator.getClass().getName());
      if (!validator.isValid()) {
        LOGGER.debug("false");
        error(context);
        return;
      }
    }
    context.next();
  }

  private void error(RoutingContext context) {
    context.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(HttpStatus.SC_BAD_REQUEST)
        .end(getBadRequestMessage().toString());
  }

  private JsonObject getBadRequestMessage() {
    return new JsonObject()
        .put("type", 400)
        .put("title", "Bad Request")
        .put("detail", "Bad query");
  }

}
