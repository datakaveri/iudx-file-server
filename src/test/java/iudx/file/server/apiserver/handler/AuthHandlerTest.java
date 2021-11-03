package iudx.file.server.apiserver.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.apiserver.handlers.AuthHandler;
import iudx.file.server.authenticator.AuthenticationService;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class AuthHandlerTest {
  @Mock
  RoutingContext event;
  @Mock
  HttpServerRequest request;
  @Mock
  HttpServerResponse response;
  @Mock
  MultiMap headers;
  MultiMap parameters;

  @BeforeEach
  public void setup() {
    doReturn(request).when(event).request();
    lenient().doReturn(response).when(event).response();
  }

  @Test
  public void testUrlBypassing() {
    AuthenticationService authService = mock(AuthenticationService.class);
    doReturn("/apis/spec").when(request).path();

    new AuthHandler(authService).handle(event);

    verify(request, times(1)).path();
    verify(event, times(1)).next();
  }

  @Test
  @DisplayName("fail - auth handler fail when null token passed")
  public void failAuthHandlerTest() {
    AuthenticationService authService = mock(AuthenticationService.class);

    doReturn("/iudx/v1/upload").when(request).path();
    doReturn(null).when(request).getHeader("token");
    doReturn(HttpMethod.POST).when(request).method();
    doReturn("asdad/asdasdsd/asdasd/dsfsdfsd/asdasdasdasd").when(request).getFormAttribute("id");

    Mockito.doReturn(response).when(response).putHeader(anyString(), anyString());
    Mockito.doReturn(response).when(response).setStatusCode(anyInt());

    new AuthHandler(authService).handle(event);

    Mockito.verify(response, times(1)).putHeader(anyString(), anyString());
    Mockito.verify(response, times(1)).setStatusCode(anyInt());
    Mockito.verify(response, times(1)).end(anyString());

  }

  @Test
  @DisplayName("success - auth handler [interospect success]")
  public void successAuthHandlerTest(Vertx vertx) {

    AuthenticationService authService = mock(AuthenticationService.class);

    doReturn("/iudx/v1/upload").when(request).path();
    doReturn("asdasds.asdasd.adasd").when(request).getHeader("token");
    doReturn(HttpMethod.POST).when(request).method();
    doReturn("asdad/asdasdsd/asdasd/dsfsdfsd/asdasdasdasd").when(request).getFormAttribute("id");

    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(true);

    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
        return null;
      }
    }).when(authService).tokenInterospect(any(), any(), any());


    new AuthHandler(authService).handle(event);

    verify(event, times(1)).next();

  }

  @Test
  @DisplayName("fail - auth handler [interospect failed]")
  public void failAuthHandlerTest(Vertx vertx) {

    AuthenticationService authService = mock(AuthenticationService.class);

    doReturn("/iudx/v1/upload").when(request).path();
    doReturn("asdasds.asdasd.adasd").when(request).getHeader("token");
    doReturn(HttpMethod.POST).when(request).method();
    doReturn("asdad/asdasdsd/asdasd/dsfsdfsd/asdasdasdasd").when(request).getFormAttribute("id");

    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(new Throwable("fail"));

    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
        return null;
      }
    }).when(authService).tokenInterospect(any(), any(), any());

    Mockito.doReturn(response).when(response).putHeader(anyString(), anyString());
    Mockito.doReturn(response).when(response).setStatusCode(anyInt());

    new AuthHandler(authService).handle(event);

    Mockito.verify(response, times(1)).putHeader(anyString(), anyString());
    Mockito.verify(response, times(1)).setStatusCode(anyInt());
    Mockito.verify(response, times(1)).end(anyString());

  }


  @Test
  @DisplayName("success - auth handler [sample file]")
  public void successAuthHandlerSampleFileTest(Vertx vertx) {

    AuthenticationService authService = mock(AuthenticationService.class);

    doReturn("/iudx/v1/upload").when(request).path();
    doReturn("asdasds.asdasd.adasd").when(request).getHeader("token");
    doReturn(HttpMethod.GET).when(request).method();
    doReturn("asdas/asdasd/sample.txt").when(request).getParam("file-id");


    new AuthHandler(authService).handle(event);

    verify(event, times(1)).next();

  }


}
