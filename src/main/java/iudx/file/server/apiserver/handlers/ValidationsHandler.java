package iudx.file.server.apiserver.handlers;

import static iudx.file.server.apiserver.utilities.Constants.APPLICATION_JSON;
import static iudx.file.server.apiserver.utilities.Constants.CONTENT_TYPE;
import java.util.List;
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
    switch (requestType) {
      case QUERY:
        List<Validator> queryValidations =
            validationFactory.create(RequestType.QUERY, parameters, headers);
        for (Validator validator : queryValidations) {
          LOGGER.debug("validator :" + validator.getClass().getName());
          if (!validator.isValid()) {
            error(context, validator);
            return;
          }
        }
        break;
      case UPLOAD:
        List<Validator> uploadValidations =
            validationFactory.create(RequestType.UPLOAD, parameters, headers);
        for (Validator validator : uploadValidations) {
          LOGGER.debug("validator :" + validator.getClass().getName());
          if (!validator.isValid()) {
            error(context, validator);
            return;
          }
        }
        break;
      case DOWNLOAD:
        List<Validator> downloadValidations =
            validationFactory.create(RequestType.DOWNLOAD, parameters, headers);
        for (Validator validator : downloadValidations) {
          LOGGER.debug("validator :" + validator.getClass().getName());
          if (!validator.isValid()) {
            error(context, validator);
            return;
          }
        }
        break;
      case DELETE:
        List<Validator> deleteValidations =
            validationFactory.create(RequestType.DELETE, parameters, headers);
        for (Validator validator : deleteValidations) {
          LOGGER.debug("validator :" + validator.getClass().getName());
          if (!validator.isValid()) {
            error(context, validator);
            return;
          }
        }
        break;
      default:
        break;
    }
    context.next();
    return;
  }

  private void error(RoutingContext context, Validator validator) {
    context.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(HttpStatus.SC_BAD_REQUEST)
        .end(getBadRequestMessage(validator.failureMessage()).toString());
  }

  private JsonObject getBadRequestMessage(String message) {
    return new JsonObject()
        .put("type", 400)
        .put("title", "Bad Request")
        .put("details", "Bad query");
  }

}
