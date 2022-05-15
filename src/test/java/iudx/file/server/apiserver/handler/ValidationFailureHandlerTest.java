package iudx.file.server.apiserver.handler;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.handlers.ValidationFailureHandler;
import iudx.file.server.apiserver.response.ResponseUrn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({ VertxExtension.class, MockitoExtension.class })
public class ValidationFailureHandlerTest {
    private static ValidationFailureHandler validationFailureHandler;
    @Mock
    RoutingContext event;
    @Mock
    HttpServerResponse httpServerResponseMock;
    @Mock
    DxRuntimeException throwableMock;
    @Mock
    RuntimeException runtimeExceptionMock;
    @Mock
    ResponseUrn responseUrnMock;
    @Mock
    Future<Void> voidFutureMock;

    @BeforeEach
    public void setUp() {
        validationFailureHandler = new ValidationFailureHandler();
    }

    @DisplayName("Test handle method when failure is DxRunTimeException")
    @Test
    public void testHandleWhenDxRuntimeException(VertxTestContext vertxTestContext) {
        when(event.failure()).thenReturn(throwableMock);
        when(throwableMock.getUrn()).thenReturn(responseUrnMock);
        when(responseUrnMock.getUrn()).thenReturn("dummy urn message");
        when(throwableMock.getStatusCode()).thenReturn(400);
        when(event.response()).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.putHeader(anyString(), anyString())).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.setStatusCode(anyInt())).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.end(anyString())).thenReturn(voidFutureMock);
        validationFailureHandler.handle(event);

        DxRuntimeException dxRuntimeException = (DxRuntimeException) event.failure();
        assertEquals(400, dxRuntimeException.getStatusCode());
        vertxTestContext.completeNow();
    }

    @DisplayName("Test handle method when failure is RuntimeException")
    @Test
    public void testHandleWhenRuntimeException(VertxTestContext vertxTestContext) {
        when(event.failure()).thenReturn(runtimeExceptionMock);
        when(event.response()).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.putHeader(anyString(), anyString())).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.setStatusCode(anyInt())).thenReturn(httpServerResponseMock);
        when(httpServerResponseMock.end(anyString())).thenReturn(voidFutureMock);
        validationFailureHandler.handle(event);

        verify(httpServerResponseMock).setStatusCode(400);
        vertxTestContext.completeNow();
    }

}
