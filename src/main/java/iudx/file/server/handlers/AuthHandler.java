package iudx.file.server.handlers;

import static iudx.file.server.utilities.Constants.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.file.server.service.AuthService;

// TODO : incomplete integration.
public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

  private DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]");

  private static AuthService authService;
  private final List<String> noUserAuthRequired = List.of("/token");

  public static AuthHandler create(AuthService authServiceImpl) {
    authService = authServiceImpl;
    return new AuthHandler();
  }

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();

    // bypassing handler for /token endpoint
    if (noUserAuthRequired.contains(request.path())) {
      context.next();
      return;
    }

    String token = request.getHeader("token");
    final String path = request.path();
    final String method = context.request().method().toString();

    // TODO : for test remove when item available in cat/ policy created in auth
    JsonObject authInfo = new JsonObject().put(API_ENDPOINT, "/ngsi-ld/v1/entities")
        .put(HEADER_TOKEN, token)
        .put(API_METHOD, method);

    JsonObject requestJson = new JsonObject().put(PARAM_ID, request.getParam("id"));

    if (token == null) {
      processUnauthorized(context, "no token");
      return;
    }

    authService.tokenInterospect(requestJson, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        LOGGER.info("auth success.");
      } else {
        LOGGER.error("Authentication failed [" + handler.cause().getMessage() + "]");
        processUnauthorized(context,
            "Authentication failed [" + handler.cause().getMessage() + "]");
        return;
      }
      context.next();
      return;
    });
  }

  private void processUnauthorized(RoutingContext ctx, String result) {
    ctx.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(HttpStatus.SC_UNAUTHORIZED).end(responseUnauthorizedJson().toString());
  }

  private JsonObject responseUnauthorizedJson() {
    return new JsonObject().put(JSON_TYPE, HttpStatus.SC_UNAUTHORIZED)
        .put(JSON_TITLE, "Not authorized").put(JSON_DETAIL,"Not authorized.");
  }

}
