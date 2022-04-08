package iudx.file.server.apiserver.handlers;

import static iudx.file.server.apiserver.utilities.Constants.*;

import iudx.file.server.apiserver.response.ResponseUrn;
import iudx.file.server.apiserver.utilities.HttpStatusCode;
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

  public AuthHandler(AuthenticationService authenticator) {
    this.authenticator=authenticator;
  }


  public static AuthHandler create(Vertx vertx) {
    authenticator = AuthenticationService.createProxy(vertx, AUTHENTICATOR_SERVICE_ADDRESS);
    return new AuthHandler(authenticator);
  }

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();

    String token = request.getHeader(HEADER_TOKEN);
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
      if (fileId == null) {// for list API
        id = request.getParam("id");
      } else {
        id = fileId.substring(0, fileId.lastIndexOf("/"));
        fileName = fileId.substring(fileId.lastIndexOf("/"));
      }
    }

    // bypass auth flow for sample file download.
    if (fileName != null && fileName.contains("sample") && "GET".equalsIgnoreCase(method)) {
      LOGGER.info("sampleFile : " + fileName);
      context.next();
      return;
    }

    LOGGER.info("fileName : " + fileName);
    LOGGER.info("id :"+id);
    JsonArray idArray = new JsonArray();
    idArray.add(id);
    JsonObject requestJson = new JsonObject().put("ids", idArray);
    if (token == null) {
      LOGGER.error("Authentication failed [no token]");
      processUnauthorized(context, true);
      return;
    }

    authInfo.put("id", id);
    authenticator.tokenInterospect(requestJson, authInfo, handler -> {
      if (handler.succeeded()) {
        LOGGER.info("auth success.");
        context.data().put("AuthResult",handler.result().getString("userID"));
      } else {
        LOGGER.error("Authentication failed [" + handler.cause().getMessage() + "]");
        processUnauthorized(context, false);
        return;
      }
      context.next();
      return;
    });
  }

  private void processUnauthorized(RoutingContext ctx, Boolean noToken) {
    ctx.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(HttpStatusCode.UNAUTHORIZED.getValue())
        .end(responseUnauthorizedJson(noToken).toString());
  }

  private JsonObject responseUnauthorizedJson(Boolean noToken) {
    return new JsonObject()
        .put(JSON_TYPE, noToken ? ResponseUrn.MISSING_TOKEN.getUrn() : ResponseUrn.INVALID_TOKEN.getUrn())
        .put(JSON_TITLE, "Not authorized")
        .put(JSON_DETAIL, noToken? ResponseUrn.MISSING_TOKEN.getMessage() : ResponseUrn.INVALID_TOKEN.getMessage());
  }

}
