package iudx.file.server.handlers;

import static iudx.file.server.utilities.Constants.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.file.server.service.TokenStore;

public class UserAuthorizationHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(UserAuthorizationHandler.class);

  private DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]");
  private static TokenStore tokenStoreClient;
  private final List<String> noUserAuthRequired = List.of("/token");

  public static UserAuthorizationHandler create(TokenStore tokenStore) {
    tokenStoreClient = tokenStore;
    return new UserAuthorizationHandler();
  }

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    
    // bypassing handler for /token endpoint
    if (noUserAuthRequired.contains(request.path())) {
      context.next();
      return;
    }

    String headerToken = request.getHeader("fileServerToken");
    String token=request.getHeader("token");

    if (headerToken == null) {
      processUnauthorized(context, "no token");
      //context.next();
      return;
    }

    LOGGER.info("token :" + token);
    tokenStoreClient.getTokenDetails(token).onComplete(handler -> {
      if (handler.succeeded()) {
        Map<String, String> dbResult = handler.result();
        String validityTimeDB = dbResult.get("validity_date");
        String file_token = dbResult.get("file_token");
        LOGGER.info("file_token"+file_token);
        LOGGER.info("validity_time"+validityTimeDB);
        LocalDateTime validityTime = LocalDateTime.parse(validityTimeDB, formatter);
        LOGGER.info("LocalDateTime validity : "+validityTime);
        LocalDateTime now = LocalDateTime.now();
        LOGGER.info("LocalDateTime now : "+now);
        LOGGER.info("LocalDateTime validity : "+validityTime);
        if (validityTime != null && now.isAfter(validityTime)) {
          LOGGER.info("Validity expired for token.");
          processUnauthorized(context, "validity expired");
          return;
        }
        if (!file_token.equals(headerToken)) {
          LOGGER.info("token doesn't match");
          processUnauthorized(context, "token doesn't match");
          return;
        }
      } else if (handler.failed()) {
        LOGGER.info("handler fail"+handler.cause());
        processUnauthorized(context, "handler failed");
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
        .put(JSON_TITLE, "Valid token required").put(JSON_DETAIL,
            "A valid token is required to access the API either token is invalid or validity expired.");
  }

}
