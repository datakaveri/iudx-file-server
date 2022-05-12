package iudx.file.server.auditing;

import static iudx.file.server.auditing.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
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
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import iudx.file.server.configuration.Configuration;
import java.sql.SQLException;

import java.util.function.Function;

@ExtendWith({ VertxExtension.class, MockitoExtension.class })
@Disabled
public class AuditingServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(AuditingServiceTest.class);
  private static AuditingService auditingService;
  private AuditingServiceImpl testAuditingService;
  // @Mock
  // private ResponseBuilder responseBuilder;
  @Mock
  private SqlConnection sqlConnection;
  private static Vertx vertxObj;
  private static JsonObject dbConfig;
  private static String databaseIP;
  private static Integer databasePort;
  private static JsonObject dummyJSON;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static Integer databasePoolSize;
  private static @Mock PgPool pgPool;
  @InjectMocks
  QueryBuilder queryBuilder;

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
    AuditingService auditingService = new AuditingServiceImpl(dbConfig, vertxObj);
    vertxTestContext.completeNow();
  }

  private JsonObject writeRequest() {
    JsonObject jsonObject = new JsonObject();
    jsonObject
        .put(API, "/ngsi-ld/v1/temporal/entities")
        .put(USER_ID, "pranav-testing-stuff")
        .put(PROVIDER_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86")
        .put(RESOURCE_ID,
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta");
    return jsonObject;
  }

  @Test
  @DisplayName("Testing write query w/o endpoint")
  void writeForMissingEndpoint(VertxTestContext vertxTestContext) {
    JsonObject request = writeRequest();
    request.remove(API);

    auditingService.executeWriteQuery(
        request,
        vertxTestContext.failing(
            response -> vertxTestContext.verify(
                () -> {
                  LOGGER
                      .debug("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                  assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                  vertxTestContext.completeNow();
                })));
  }

  @Test
  @DisplayName("Testing write query w/o user ID")
  void writeForMissingUserID(VertxTestContext vertxTestContext) {
    JsonObject request = writeRequest();
    request.remove(USER_ID);
    auditingService.executeWriteQuery(
        request,
        vertxTestContext.failing(
            response -> vertxTestContext.verify(
                () -> {
                  LOGGER
                      .debug("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                  assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                  vertxTestContext.completeNow();
                })));
  }

  @Test
  @DisplayName("Testing write query w/o RESOURCE ID")
  void writeForMissingResourceid(VertxTestContext vertxTestContext) {
    JsonObject request = writeRequest();
    request.remove(RESOURCE_ID);
    auditingService.executeWriteQuery(
        request,
        vertxTestContext.failing(
            response -> vertxTestContext.verify(
                () -> {
                  LOGGER
                      .debug("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                  assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                  vertxTestContext.completeNow();
                })));
  }

  @Test
  @DisplayName("Testing write query w/o PROVIDER ID")
  void writeForMissingProviderid(VertxTestContext vertxTestContext) {
    JsonObject request = writeRequest();
    request.remove(PROVIDER_ID);

    AuditingService auditingService = mock(AuditingService.class);
    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
    JsonObject jsonObject = mock(JsonObject.class);

    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(new Throwable("fail"));

    when(queryBuilder.buildWriteQuery(any())).thenReturn(new JsonObject().put(ERROR, "error"));

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

  @Test
  @DisplayName("Testing executeWriteQuery method with valid query here")
  public void testExecuteWriteQuery(VertxTestContext vertxTestContext) {
    JsonObject request = writeRequest();
    AuditingServiceImpl auditingService2 = new AuditingServiceImpl(dbConfig, vertxObj);
    assertNotNull(auditingService2.executeWriteQuery(request, AsyncResult::succeeded));
    vertxTestContext.completeNow();
  }

  @DisplayName("Test query containing error in executeWriteQuery")
  @Test
  public void testQueryErrorInExecuteWriteQuery(VertxTestContext vertxTestContext) {
    dummyJSON = new JsonObject();
    dummyJSON.put("username", "ABC");
    dummyJSON.put("password", "ABC");

    AuditingServiceImpl auditingService = new AuditingServiceImpl(dbConfig, vertxObj);
    AuditingService res = auditingService.executeWriteQuery(dummyJSON, AsyncResult::succeeded);
    assertNull(res);
    vertxTestContext.completeNow();
  }

  @DisplayName("Tests rows succeeded or failed in writeInDatabase method")
  @Test
  public void testCanWriteInDatabase(VertxTestContext vertxTestContext) throws SQLException {
    JsonObject request = writeRequest();
    testAuditingService = new AuditingServiceImpl(dbConfig, vertxObj);
    testAuditingService.writeInDatabase(queryBuilder.buildWriteQuery(request));
    PgPool poolMock = mock(PgPool.class);
    Future<RowSet<Row>> rowSetFutureMock = mock(Future.class);
    Query<RowSet<Row>> queryRowSetRowMock = mock(Query.class);
    // Query<RowSet<Row>> castRowSetFutureMock = (Query<RowSet<Row>>)
    // mock(rowSetFutureMock.getClass());
    JsonObject query = queryBuilder.buildWriteQuery(request);
    Future<SqlConnection> sqlConnectionFuture = mock(Future.class);
    SqlConnection sqlConnectionMock = mock(SqlConnection.class);
    AsyncResult<RowSet<Row>> futureAsyncRowSet_Compose = mock(AsyncResult.class);
    AsyncResult<io.vertx.sqlclient.RowSet<io.vertx.sqlclient.Row>> row = mock(AsyncResult.class);
    /**
     * JoinRowSet joinRowSetMock = mock(JoinRowSet.class);
     * Future joinRowSetMockFuture = (Future) mock(joinRowSetMock.getClass());
     **/
    // when(poolMock.getConnection()).thenReturn(Future.succeededFuture());
    lenient().when(poolMock.getConnection()).thenReturn(sqlConnectionFuture);
    // lenient().when(sqlConnectionMock.query(anyString())).thenReturn(queryRowSetRowMock);
    lenient().when(sqlConnectionMock.query(anyString())).thenReturn(queryRowSetRowMock);
    // when(sqlConnectionFuture.compose(any())).thenReturn(AsyncResult<RowSet<Row>>::succeeded);
    // doReturn(rowSetFutureMock.map("")).when(sqlConnectionFuture.compose(any()));
    // when(sqlConnectionFuture.compose(any())).thenReturn();
    Future mocks = mock(rowSetFutureMock.getClass());

    when(sqlConnectionFuture.compose(any())).thenReturn(mocks);

    // lenient().when(sqlConnectionFuture.compose(any())).thenReturn((Future<Object>)
    // row.result());
    // when(mocks.onComplete(any())).thenReturn(); ---> I shouldn't return anything
    // here
    // it should go inside onComplete
    System.out.println(sqlConnectionFuture.result());
    // assertEquals(" ", sqlConnectionMock.query(anyString()));
    // assertEquals(" ", poolMock.getConnection());
    // assertEquals(" ", sqlConnectionFuture.compose(connection ->
    // connection.query(query.getString(QUERY_KEY)).execute()));
    // assertEquals("", poolMock.getConnection().compose(any()));
    // assertEquals(" ", sqlConnectionFuture.compose(connection ->
    // connection.query(query.getString(QUERY_KEY)).execute()));
    assertEquals(null, sqlConnectionFuture.compose(connection -> connection.query(query.getString(QUERY_KEY)).execute())
        .onComplete(handler -> {
          System.out.println("I'm here !!");
        }));

    System.out.println(sqlConnectionFuture.compose(connection -> connection.query(query.getString(QUERY_KEY)).execute())
        .isComplete());
    vertxTestContext.completeNow();
  }
}
