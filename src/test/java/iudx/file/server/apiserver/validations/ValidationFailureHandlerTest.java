package iudx.file.server.apiserver.validations;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.BadRequestException;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ValidationFailureHandlerTest {
    private static ValidationFailureHandler validationFailureHandler;
    @Mock
    BadRequestException badRequestExceptionMock;
    @Mock
    RoutingContext routingContext;
    @Mock
    HttpServerResponse httpServerResponseMock;
    @Mock
    Future<Void> voidFutureMock;

    @BeforeEach
    public void setUp(VertxTestContext testContext) {
        validationFailureHandler = new ValidationFailureHandler();
        testContext.completeNow();
    }

    @Test
    @DisplayName("Test handle method ")
    public void testHandle(VertxTestContext vertxTestContext) {
        when(routingContext.failure()).thenReturn(badRequestExceptionMock);
        when(routingContext.response()).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.putHeader(anyString(), anyString())).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.setStatusCode(anyInt())).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.end(anyString())).thenReturn(voidFutureMock);
        validationFailureHandler.handle(routingContext);
        verify(httpServerResponseMock).setStatusCode(400);
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("type", HttpStatus.SC_BAD_REQUEST);
        jsonObject.put("title", "Bad Request");
        jsonObject.put("detail", "Bad query");
        verify(httpServerResponseMock).end(jsonObject.encode());
        vertxTestContext.completeNow();
    }

}
