package iudx.file.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.file.server.auditing.util.Constants.FAILED;
import static iudx.file.server.auditing.util.Constants.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class DataBrokerServiceImplTest {
  static DataBrokerServiceImpl databroker;
  static RabbitMQClient rmqClientMock;
  static AsyncResult<Void> asyncResult;
  static Throwable throwable;

  @BeforeAll
  @DisplayName("Initialize vertx and deploy verticle")
  public static void init(Vertx vertx, VertxTestContext testContext) {

    asyncResult = mock(AsyncResult.class);
    rmqClientMock = mock(RabbitMQClient.class);
    throwable = mock(Throwable.class);
    AsyncResult<Void> voidAsyncResult = mock(AsyncResult.class);

    when(voidAsyncResult.succeeded()).thenReturn(true);
    Mockito.doAnswer(
            new Answer<AsyncResult<Void>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(rmqClientMock)
        .start(any());

    databroker = new DataBrokerServiceImpl(rmqClientMock);
    testContext.completeNow();
  }

  @Test
  @DisplayName("Success: test publish message")
  void successfulPublishMessageTest(VertxTestContext testContext) {

    when(rmqClientMock.isConnected()).thenReturn(true);
    when(asyncResult.succeeded()).thenReturn(true);

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(asyncResult);
                return null;
              }
            })
        .when(rmqClientMock)
        .basicPublish(anyString(), anyString(), (Buffer) any(), any());

    databroker.publishMessage(
        new JsonObject(),
        "auditing",
        "##",
        handler -> {
          if (handler.succeeded()) {
            assertEquals(SUCCESS, handler.result().getString("type"));
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @DisplayName("Fail: publish message")
  void failPublishMessageTest(VertxTestContext testContext) {
    when(rmqClientMock.isConnected()).thenReturn(true);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(3)).handle(asyncResult);
                return null;
              }
            })
        .when(rmqClientMock)
        .basicPublish(anyString(), anyString(), (Buffer) any(), any());

    databroker.publishMessage(
        new JsonObject(),
        "auditing",
        "##",
        handler -> {
          if (handler.succeeded()) {
            testContext.failNow("Fail");
          } else {
            JsonObject jsonObject = new JsonObject(handler.cause().getMessage());
            assertEquals(jsonObject.getString("title"), FAILED);
            testContext.completeNow();
          }
        });
  }
}
