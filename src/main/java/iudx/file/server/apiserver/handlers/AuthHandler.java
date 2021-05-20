package iudx.file.server.apiserver.handlers;

import static iudx.file.server.apiserver.utilities.Constants.*;
import java.util.List;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.file.server.authenticator.AuthenticationService;

public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);
  private static final String AUTHENTICATOR_SERVICE_ADDRESS = AUTH_SERVICE_ADDRESS;

  private static AuthenticationService authenticator;
  private final List<String> noUserAuthRequired = List.of("/apis/specs", "/apis");

  public static AuthHandler create(Vertx vertx) {
    authenticator = AuthenticationService.createProxy(vertx, AUTHENTICATOR_SERVICE_ADDRESS);
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
    LOGGER.debug("path : " + path);
    final String method = context.request().method().toString();

    JsonObject authInfo = new JsonObject().put(API_ENDPOINT, path)
        .put(HEADER_TOKEN, token)
        .put(API_METHOD, method);


    String id = null;
    String fileName = null;
    if ("POST".equalsIgnoreCase(method)) {
      id = request.getFormAttribute("id");
    } else if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
      String fileId = request.getParam("file-id");
      if(fileId==null) {//for list API
        id=request.getParam("id");
      }else {
        id = fileId.substring(0, fileId.lastIndexOf("/"));
        fileName = fileId.substring(fileId.lastIndexOf("/"));
      }
    }

    // bypass auth flow for sample file download.
    if (fileName != null && fileName.contains("sample")) {
      LOGGER.info("sampleFile : " + fileName);
      context.next();
      return;
    }

    LOGGER.info("fileName : " + fileName);

    JsonArray idArray = new JsonArray();
    idArray.add(id);
    JsonObject requestJson = new JsonObject().put("ids", idArray);
    if (token == null) {
      processUnauthorized(context, "no token");
      return;
    }

    authenticator.tokenInterospect(requestJson, authInfo, handler -> {
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
        .put(JSON_TITLE, "Not authorized").put(JSON_DETAIL, "Not authorized.");
  }

}
