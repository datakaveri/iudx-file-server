package iudx.file.server.apiserver.validations;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.BadRequestException;

//TODO : make usable after looking for Exception propogation in vertx
public class ValidationFailureHandler implements Handler<RoutingContext>{

  private static final Logger LOGGER = LogManager.getLogger(ValidationFailureHandler.class);
  @Override
  public void handle(RoutingContext context) {
    Throwable failure = context.failure();
    failure.printStackTrace();
    LOGGER.error("error :"+failure);
    if (failure instanceof BadRequestException) { 
      // Something went wrong during validation!
      //String failedParameter=((BadRequestException) failure);
      failure.printStackTrace();
      LOGGER.error("error :" +failure.getLocalizedMessage()+" param : ");
      String validationErrorMessage = "Bad Query";
      context.response()
        .putHeader("content-type", "application/json")
        .setStatusCode(HttpStatus.SC_BAD_REQUEST)
        .end(validationFailureReponse(validationErrorMessage).toString());
    }
    context.next();
    return;
  }
  
  private JsonObject validationFailureReponse(String message) {
    return new JsonObject()
        .put("type", HttpStatus.SC_BAD_REQUEST)
        .put("title", "Bad Request")
        .put("detail", "Bad query");
  }
}
