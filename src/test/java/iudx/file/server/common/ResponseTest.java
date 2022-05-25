package iudx.file.server.common;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.apiserver.response.ResponseUrn;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith({VertxExtension.class, MockitoExtension.class})
/**
 * Testing class with Builder Design pattern: Response
 */
public class ResponseTest {
    private String type;
    private int status;
    private String title;
    private String detail;
    @Mock
    Throwable failureHandler;


    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        type = ResponseUrn.MISSING_TOKEN.getUrn();
        status = HttpStatus.SC_NOT_FOUND;
        title = ResponseUrn.MISSING_TOKEN.getMessage();
        detail = failureHandler.getLocalizedMessage();
        vertxTestContext.completeNow();
    }

    @DisplayName("Test withUrn in Builder")
    @Test
    public void testWithUrn(VertxTestContext vertxTestContext) {

        Response response = new Response.Builder()
                .withUrn(type)
                .withTitle(title)
                .withStatus(status)
                .withDetail(detail).build();

        JsonObject expected = new JsonObject()
                .put("type", response.getType())
                .put("status", response.getStatus())
                .put("title", response.getTitle())
                .put("detail", response.getDetail());

        String actual = response.toString();
        Assertions.assertEquals(expected.encode(), actual);
        vertxTestContext.completeNow();
    }

}
