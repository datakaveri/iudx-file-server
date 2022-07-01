package iudx.file.server.apiserver.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

import java.util.Map;
import java.util.stream.Stream;

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
  @Mock
  HttpMethod httpMethodMock;
  @Mock
  AuthenticationService authService;
  @Mock
  AsyncResult<JsonObject> asyncResult;


  @BeforeEach
  public void setup() {
    lenient().doReturn(request).when(event).request();
    lenient().doReturn(response).when(event).response();
  }

  @Test
  @DisplayName("fail - auth handler fail when null token passed")
  public void failAuthHandlerTest() {
    doReturn("/iudx/v1/upload").when(request).path();
    doReturn(null).when(request).getHeader("token");
    doReturn(HttpMethod.POST).when(request).method();
    //doReturn("asdad/asdasdsd/asdasd/dsfsdfsd/asdasdasdasd").when(request).getFormAttribute("id");

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

    doReturn("/iudx/v1/upload").when(request).path();
    doReturn("asdasds.asdasd.adasd").when(request).getHeader("token");
    doReturn(HttpMethod.POST).when(request).method();
    doReturn("asdad/asdasdsd/asdasd/dsfsdfsd/asdasdasdasd").when(request).getFormAttribute("id");

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(new JsonObject().put("userID","aasadas"));

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

    doReturn("/iudx/v1/upload").when(request).path();
    doReturn("asdasds.asdasd.adasd").when(request).getHeader("token");
    doReturn(HttpMethod.POST).when(request).method();
    doReturn("asdad/asdasdsd/asdasd/dsfsdfsd/asdasdasdasd").when(request).getFormAttribute("id");
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

  private static Stream<Arguments> inputValues()
  {
    return Stream.of(
            Arguments.of("GET",null),
            Arguments.of("DELETE","dummy/file/ID")
    );
  }

  @ParameterizedTest(name = "{index}) method = {0}, fileId = {1}")
  @MethodSource("inputValues")
  @DisplayName("Test handle method with inputValues")
  public void testHandle(String method, String fileID,VertxTestContext vertxTestContext)
  {

    Map<String,Object> stringObjectMapMock = mock(Map.class);

    JsonObject jsonObject = new JsonObject();
    jsonObject.put("userID", "Dummy User ID");

    when(event.request()).thenReturn(request);
    when(request.path()).thenReturn("Dummy Request Path");
    when(request.getHeader(anyString())).thenReturn("Dummy Token");
    when(request.method()).thenReturn(httpMethodMock);
    when(httpMethodMock.toString()).thenReturn(method);
    when(request.getParam(anyString())).thenReturn(fileID);
    when(asyncResult.succeeded()).thenReturn(true);
    when(event.data()).thenReturn(stringObjectMapMock);
    when(asyncResult.result()).thenReturn(jsonObject);

    doAnswer(new Answer<AsyncResult<JsonObject>>(){
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable{
        ((Handler<AsyncResult<JsonObject>>)arg0.getArgument(2)).handle(asyncResult);
        return null;
      }
    }).when(authService).tokenInterospect(any(),any(),any());

    AuthHandler authHandler = new AuthHandler(authService);
    authHandler.handle(event);
    verify(event,times(2)).request();
    verify(authService, times(1)).tokenInterospect(any(),any(),any());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test static method: create")
  public void testCreate(VertxTestContext vertxTestContext)
  {
    AuthHandler res =  AuthHandler.create(Vertx.vertx());
    assertNotNull(res);
    vertxTestContext.completeNow();
  }
  
  

  @Test
  @DisplayName("bypass - auth handler [bypass for sample file]")
  public void bypassAuthHandlerTest(Vertx vertx) {

    doReturn("/iudx/v1/upload").when(request).path();
    doReturn("asdasds.asdasd.adasd").when(request).getHeader("token");
    doReturn(HttpMethod.POST).when(request).method();
    doReturn("asdad/asdasdsd/asdasd/dsfsdfsd/asdasdasdasd").when(request).getFormAttribute("id");
    doReturn("sample.txt").when(request).getFormAttribute("file");
    
    new AuthHandler(authService).handle(event);

    verify(event, times(1)).next();
    verify(authService,times(0)).tokenInterospect(any(), any(), any());
    

  }

}
