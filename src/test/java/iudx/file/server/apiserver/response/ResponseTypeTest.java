package iudx.file.server.apiserver.response;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
public class ResponseTypeTest {

    @ParameterizedTest
    @EnumSource
    public void test(ResponseType responseType,VertxTestContext vertxTestContext)
    {
        assertNotNull(responseType);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test for a single enum")
    public void testEnumInternalError( VertxTestContext vertxTestContext)
    {
        assertEquals("Internal error",ResponseType.InternalError.getMessage());
        assertEquals(500,ResponseType.InternalError.getCode());
        assertEquals(ResponseType.InternalError, ResponseType.fromCode(500));
        vertxTestContext.completeNow();
    }
}
