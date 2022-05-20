package iudx.file.server.apiserver.validations;

import static iudx.file.server.apiserver.utilities.Constants.PARAM_COORDINATES;
import static iudx.file.server.apiserver.utilities.Constants.PARAM_END_TIME;
import static iudx.file.server.apiserver.utilities.Constants.PARAM_GEOMETRY;
import static iudx.file.server.apiserver.utilities.Constants.PARAM_START_TIME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class RequestValidatorTest {

  static RequestValidator requestValidator;
  static ContentTypeValidator contentTypeValidator;
  static MultiMap map;
  static JsonObject allowedContentType = new JsonObject("{\n" +
      "                \"text/plain\": \"txt\",\n" +
      "                \"text/csv\": \"csv\",\n" +
      "                \"application/pdf\": \"pdf\",\n" +
      "                \"video/mp4\": \"mp4\",\n" +
      "                \"application/zip\": \"zip\",\n" +
      "                \"application/x-7z-compressed\": \"7z\",\n" +
      "                \"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\": \"xlsx\",\n" +
      "                \"application/vnd.openxmlformats-officedocument.wordprocessingml.document\": \"docx\"\n" +
      "            }");

  @BeforeAll
  @DisplayName("Initialize Vertx and deploy Auth Verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {
    requestValidator = new RequestValidator();
    contentTypeValidator = new ContentTypeValidator(allowedContentType);
    testContext.completeNow();
  }

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    map = MultiMap.caseInsensitiveMultiMap();
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("success - is valid archieve query")
  public void success4ValidArchiveReq(VertxTestContext testContext) {
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
  @DisplayName("fail - is valid archive query")
  public void fail4InvalidArchiveReq(VertxTestContext testContext) {
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

  @Test
  @DisplayName("Invalid content type")
  public void invalidContentType() {
    assertFalse(contentTypeValidator.isValid("application/jsonp"));
  }

  @Test
  @DisplayName("valid content type")
  public void validContentType() {
    assertTrue(contentTypeValidator.isValid("text/plain"));
  }

  @Test
  @DisplayName("test isValidArchiveRequest when param does not contain PARAM_GEOMETRY")
  public void IsValidArchiveRequestWithoutGeometry(VertxTestContext vertxTestContext) {
    map.add(PARAM_COORDINATES, "[25.89,76.89]");
    map.add(PARAM_START_TIME, "");
    map.add(PARAM_END_TIME, "");
    requestValidator.isValidArchiveRequest(map).onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow(handler.cause());

      }
      if (handler.failed()) {
        vertxTestContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("test isValidArchiveRequest param does not contain PARAM_START_TIME")
  public void IsValidArchiveRequestWithoutStartTime(VertxTestContext vertxTestContext) {
    map.add(PARAM_GEOMETRY, "point");
    map.add(PARAM_COORDINATES, "[25.89,76.89]");
    map.add(PARAM_END_TIME, "");
    requestValidator.isValidArchiveRequest(map).onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow(handler.cause());
      } else {
        vertxTestContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("test isValidArchiveRequest when param does not contain PARAM_END_TIME ")
  public void isValidArchiveRequestWithoutEndTime(VertxTestContext vertxTestContext) {
    map.add(PARAM_GEOMETRY, "point");
    map.add(PARAM_COORDINATES, "[25.89,76.89]");
    map.add(PARAM_START_TIME, "");
    requestValidator.isValidArchiveRequest(map).onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow(handler.cause());
      } else {
        vertxTestContext.completeNow();
      }
    });
  }
}
