package iudx.file.server.auditing;

import static iudx.file.server.auditing.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.future.FailedFuture;
import io.vertx.core.impl.future.SucceededFuture;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import iudx.file.server.auditing.util.QueryBuilder;
import iudx.file.server.auditing.util.ResponseBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.join.QueryBitSetProducer;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import iudx.file.server.configuration.Configuration;

import java.util.function.Function;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
@Disabled
public class AuditingServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(AuditingServiceTest.class);
  private static AuditingService auditingService;
//  @Mock
//  private ResponseBuilder responseBuilder;
  @Mock
  private SqlConnection sqlConnection;
  private static Vertx vertxObj;
  private static JsonObject dbConfig;
  private static String databaseIP;
  private static Integer databasePort;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static Integer databasePoolSize;
  private static 
  @Mock
  PgPool pgPool;
  @InjectMocks
  QueryBuilder queryBuilder;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
    vertxObj = vertx;
    dbConfig = new JsonObject();
    dbConfig.put("auditingDatabaseIP","52.140.124.157");
    dbConfig.put("auditingDatabasePort", 32508);
    dbConfig.put("auditingDatabaseName","metering");
    dbConfig.put("auditingDatabaseUserName","immudb");
    dbConfig.put("auditingDatabasePassword","IzY=Q8^~dzTkdv&v0Cc");
    dbConfig.put("auditingDatabaseTableName","rsaudit");
    dbConfig.put("auditingPoolSize", 24);
    AuditingService auditingService = new AuditingServiceImpl(dbConfig, vertxObj);
    vertxTestContext.completeNow();
  }

  private JsonObject writeRequest() {
    JsonObject jsonObject = new JsonObject();
    jsonObject
            .put(API,"/ngsi-ld/v1/temporal/entities")
            .put(USER_ID,"pranav-testing-stuff")
            .put(PROVIDER_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86")
            .put(RESOURCE_ID,"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta")
            .put(RESPONSE_SIZE,12345);
    return jsonObject;
  }

  @Test
  @DisplayName("Testing write query w/o endpoint")
  void writeForMissingEndpoint(VertxTestContext vertxTestContext){
    JsonObject request = writeRequest();
    request.remove(API);

    auditingService.executeWriteQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
                  LOGGER
                      .debug("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
              assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Testing write query w/o user ID")
  void writeForMissingUserID(VertxTestContext vertxTestContext){
    JsonObject request = writeRequest();
    request.remove(USER_ID);
    auditingService.executeWriteQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
                  LOGGER
                      .debug("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
              assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Testing write query w/o RESOURCE ID")
  void writeForMissingResourceid(VertxTestContext vertxTestContext){
    JsonObject request = writeRequest();
    request.remove(RESOURCE_ID);
    auditingService.executeWriteQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
          () -> {
                  LOGGER
                      .debug("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
            assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
            vertxTestContext.completeNow();
          })));
  }

  @Test
  @DisplayName("Testing write query w/o PROVIDER ID")
  void writeForMissingProviderid(VertxTestContext vertxTestContext){
    JsonObject request = writeRequest();
    request.remove(PROVIDER_ID);
    
    AuditingService auditingService = mock(AuditingService.class);
    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
    JsonObject jsonObject = mock(JsonObject.class);
    
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(new Throwable("fail"));
    
    when(queryBuilder.buildWriteQuery(any())).thenReturn(new JsonObject().put(ERROR,"error"));
    
    when(responseBuilder.setTypeAndTitle(anyInt())).thenReturn(responseBuilder);
    when(responseBuilder.setMessage(anyString())).thenReturn(responseBuilder);
    when(responseBuilder.getResponse()).thenReturn(new JsonObject());
    
    doReturn(jsonObject).when(responseBuilder).getResponse();
    doReturn("fail").when(jsonObject).toString();
    
    
    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(auditingService).executeWriteQuery(any(), any());

    auditingService.executeWriteQuery(eq(request), any());

    verify(responseBuilder, times(1)).setTypeAndTitle(anyInt());
    verify(responseBuilder, times(1)).setMessage(any());
  }

  @Test
  @DisplayName("Testing Write Query")
  void writeData(VertxTestContext vertxTestContext) {
    JsonObject request = writeRequest();
    AuditingService auditingService = mock(AuditingService.class);
    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

    when(asyncResult.succeeded()).thenReturn(true);
    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(auditingService).executeWriteQuery(any(), any());

    auditingService.executeWriteQuery(request, any());

   verify(responseBuilder, times(0)).setTypeAndTitle(anyInt());
   verify(responseBuilder, times(0)).setMessage(any());
  }
}
