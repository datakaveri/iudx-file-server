package iudx.file.server.apiserver.validations;

import static iudx.file.server.apiserver.utilities.Constants.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class RequestValidatorTest {

  static RequestValidator requestValidator;

  @BeforeAll
  @DisplayName("Initialize Vertx and deploy Auth Verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {
    requestValidator = new RequestValidator();
    testContext.completeNow();
  }


  @Test
  @DisplayName("success - is valid archieve query")
  public void success4ValidArchiveReq(VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(PARAM_GEOMETRY, "point");
    map.add(PARAM_COORDINATES, "[25.89,76.89]");
    map.add(PARAM_START_TIME, "");
    map.add(PARAM_END_TIME, "");


    requestValidator.isValidArchiveRequest(map).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("failed for valid query");
      }
    });

  }

  @Test
  @DisplayName("fail - is valid archieve query")
  public void fail4InvalidArchiveReq(VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(PARAM_GEOMETRY, "point");
    map.add(PARAM_START_TIME, "");
    map.add(PARAM_END_TIME, "");


    requestValidator.isValidArchiveRequest(map).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("success for invalid query");
      } else {
        testContext.completeNow();
      }
    });
  }


  @Test
  @DisplayName("success - valid params in query")
  public void success4ValidQueryParams(VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(PARAM_GEOMETRY, "point");
    map.add(PARAM_START_TIME, "");
    map.add(PARAM_END_TIME, "");


    requestValidator.isValid(map).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("fail for valid query");
      }
    });
  }


  @Test
  @DisplayName("fail - invalid params in query")
  public void fail4InvalidQueryParams(VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add("invalid param", "1=1");
    map.add(PARAM_START_TIME, "");
    map.add(PARAM_END_TIME, "");


    requestValidator.isValid(map).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("success for invalid query");

      } else {
        testContext.completeNow();
      }
    });
  }

}
