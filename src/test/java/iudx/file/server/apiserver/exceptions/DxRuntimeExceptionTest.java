package iudx.file.server.apiserver.exceptions;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.apiserver.response.ResponseUrn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class DxRuntimeExceptionTest {

    @Test
    @DisplayName("Test constructor ")
    public void testConstructor(VertxTestContext vertxTestContext)
    {
        int statusCode = 400;
        ResponseUrn responseUrn = ResponseUrn.INVALID_GEO_PARAM;
        DxRuntimeException obj = new DxRuntimeException(statusCode,responseUrn);
        assertEquals(400,obj.getStatusCode());
        assertEquals(ResponseUrn.INVALID_GEO_PARAM, obj.getUrn());
        assertEquals(ResponseUrn.INVALID_GEO_PARAM.getMessage(), obj.getMessage());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test constructor for Throwable RuntimeException")
    public void testConstructorForRuntimeException(VertxTestContext vertxTestContext)
    {
        int statusCode = 400;
        ResponseUrn responseUrn = ResponseUrn.INVALID_OPERATION;
        RuntimeException runtimeException = new RuntimeException("failed");
        DxRuntimeException obj = new DxRuntimeException(statusCode, responseUrn, runtimeException);
        assertEquals(400, obj.getStatusCode());
        assertEquals(ResponseUrn.INVALID_OPERATION,obj.getUrn());
        assertEquals(ResponseUrn.INVALID_OPERATION.getMessage(),obj.getMessage());
        vertxTestContext.completeNow();
    }

}
