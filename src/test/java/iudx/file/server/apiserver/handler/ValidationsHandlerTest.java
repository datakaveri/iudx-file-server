package iudx.file.server.apiserver.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static iudx.file.server.apiserver.utilities.Constants.*;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Any;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import iudx.file.server.apiserver.handlers.ValidationsHandler;
import iudx.file.server.apiserver.validations.RequestType;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class ValidationsHandlerTest {
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
    Mockito.doReturn(request).when(event).request();
    lenient().doReturn(response).when(event).response();
  }

  @Test
  public void validationHandlerSuccess() {
    parameters = MultiMap.caseInsensitiveMultiMap();
    parameters.set(PARAM_ID, "b58da193-23d9-43eb-b98a-a103d4b6103c");

    Mockito.doReturn(parameters).when(request).params();
    Mockito.doReturn(headers).when(request).headers();

    new ValidationsHandler(RequestType.LIST_QUERY).handle(event);
    Mockito.verify(event, times(1)).next();
  }

  @Test
  public void validationHandlerFailed() {
    parameters = MultiMap.caseInsensitiveMultiMap();
    parameters.set(PARAM_ID, "asdasd/asdasd");

    Mockito.doReturn(parameters).when(request).params();
    Mockito.doReturn(headers).when(request).headers();

    ValidationsHandler validationsHandler = new ValidationsHandler(RequestType.LIST_QUERY);
    assertThrows(DxRuntimeException.class, () -> validationsHandler.handle(event));
  }

  

}
