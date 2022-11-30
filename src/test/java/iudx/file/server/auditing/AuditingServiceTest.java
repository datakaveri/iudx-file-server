package iudx.file.server.auditing;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.databroker.DataBrokerService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static iudx.file.server.auditing.util.Constants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class AuditingServiceTest {
  private static Vertx vertxObj;
  private static JsonObject dbConfig;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
    vertxObj = vertx;
    dbConfig = new JsonObject();
    dbConfig.put("auditingDatabaseIP", "localhost");
    dbConfig.put("auditingDatabasePort", 123);
    dbConfig.put("auditingDatabaseName", "auditing");
    dbConfig.put("auditingDatabaseUserName", "immudb");
    dbConfig.put("auditingDatabasePassword", "");
    dbConfig.put("auditingPoolSize", 24);
    dbConfig.put("auditingDatabaseTableName", "tableName");
    AuditingService auditingService = new AuditingServiceImpl(dbConfig, vertxObj);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Testing Write Query Successful")
  void writeDataSuccessful(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    long time = zst.toInstant().toEpochMilli();
    String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
    request.put(EPOCH_TIME, time);
    request.put(ISO_TIME, isoTime);
    request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    request.put(ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
    request.put(API, "/ngsi-ld/v1/subscription");
    request.put(RESPONSE_SIZE, 12);
    AuditingServiceImpl auditingService = new AuditingServiceImpl(dbConfig, vertxObj);

    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    AuditingServiceImpl.rmqService = mock(DataBrokerService.class);

    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
                return null;
              }
            })
        .when(auditingService.rmqService)
        .publishMessage(any(), anyString(), anyString(), any());

    auditingService.executeWriteQuery(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Failed");
          }
        });
    verify(auditingService.rmqService, times(1))
        .publishMessage(any(), anyString(), anyString(), any());
  }
}
