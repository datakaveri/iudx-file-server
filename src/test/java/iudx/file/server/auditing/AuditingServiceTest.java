package iudx.file.server.auditing;

import static iudx.file.server.auditing.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;


@ExtendWith({VertxExtension.class, MockitoExtension.class})

public class AuditingServiceTest {


    private static Vertx vertxObj;
    private static JsonObject dbConfig;
    private static JsonObject dummyJSON;


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


    @DisplayName("Test executeWriteQuery Success")
    @Test
    public void testExecuteWriteQuerySuccess(VertxTestContext vertxTestContext) {

        JsonObject request = writeRequest();
        AuditingServiceImpl auditingServiceImpl = new AuditingServiceImpl(dbConfig, vertxObj);
        Future<SqlConnection> sqlConnectionFuture = mock(Future.class);
        Future<Object> objectFuture = mock(Future.class);
        AsyncResult<RowSet<Row>> asyncResultMock = mock(AsyncResult.class);

        auditingServiceImpl.pool = mock(PgPool.class);

        when(auditingServiceImpl.pool.getConnection()).thenReturn(sqlConnectionFuture);
        when(sqlConnectionFuture.compose(any())).thenReturn(objectFuture);
        when(asyncResultMock.succeeded()).thenReturn(true);

        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResultMock);
                return null;
            }
        }).when(objectFuture).onComplete(any());

        JsonObject expected = new JsonObject();
        expected.put("type", 200);
        expected.put("title", "Success");
        expected.put("detail", "Table Updated Successfully");

        auditingServiceImpl.executeWriteQuery(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });

    }

    @DisplayName("Test executeWriteQuery Failure")
    @Test
    public void testExecuteWriteQueryFailure(VertxTestContext vertxTestContext) {

        AuditingServiceImpl auditingServiceImpl = new AuditingServiceImpl(dbConfig, vertxObj);
        JsonObject request = writeRequest();
        Future<SqlConnection> sqlConnectionFuture = mock(Future.class);
        Future<Object> objectFutureMock = mock(Future.class);
        AsyncResult<RowSet<Row>> asyncResultMock = mock(AsyncResult.class);
        Throwable throwableMock = mock(Throwable.class);

        auditingServiceImpl.pool = mock(PgPool.class);

        when(auditingServiceImpl.pool.getConnection()).thenReturn(sqlConnectionFuture);
        when(sqlConnectionFuture.compose(any())).thenReturn(objectFutureMock);
        when(asyncResultMock.failed()).thenReturn(true);
        when(asyncResultMock.cause()).thenReturn(throwableMock);
        when(throwableMock.getMessage()).thenReturn("Dummy Failure Message");
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResultMock);
                return null;
            }
        }).when(objectFutureMock).onComplete(any());


        auditingServiceImpl.executeWriteQuery(request, handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded while there is failure in fetching rows ");
            } else {
                assertNull(handler.result());
                String expected = "io.vertx.core.impl.NoStackTraceThrowable: {\"type\":400,\"title\":\"Failed\",\"detail\":\"Dummy Failure Message\"}";
                assertEquals(expected, handler.cause().toString());
                vertxTestContext.completeNow();
            }
        });
    }
}
