package iudx.file.server.auditing.util;

import static iudx.file.server.auditing.util.Constants.FAILED;
import static iudx.file.server.auditing.util.Constants.SUCCESS;
import static junit.framework.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.json.JsonArray;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ResponseBuilderTest {
  ResponseBuilder responseBuilder;

  @Test
  @DisplayName("test typeAndTitle method - title 'success'")
  void testTypeAndTitle(VertxTestContext testContext) {
    String status = SUCCESS;
    int statusCode = 200;
    responseBuilder = new ResponseBuilder(status);
    assertNotNull(responseBuilder.setTypeAndTitle(statusCode));
    testContext.completeNow();
  }

  @Test
  @DisplayName("test typeAndTitle method - title 'failed'")
  void testTypeAndTitle2(VertxTestContext testContext) {
    String status = FAILED;
    int statusCode = 400;
    responseBuilder = new ResponseBuilder(status);
    assertNotNull(responseBuilder.setTypeAndTitle(statusCode));
    testContext.completeNow();
  }

  @Test
  @DisplayName("test setJsonArray method")
  void testSetJsonArray(VertxTestContext testContext) {
    String status = "dummy";
    JsonArray jsonArray = new JsonArray();
    responseBuilder = new ResponseBuilder(status);
    assertNotNull(responseBuilder.setJsonArray(jsonArray));
    testContext.completeNow();
  }
}
