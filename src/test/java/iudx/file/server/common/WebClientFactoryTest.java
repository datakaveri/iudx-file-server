package iudx.file.server.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.impl.WebClientBase;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@ExtendWith({ VertxExtension.class, MockitoExtension.class })
public class WebClientFactoryTest {

    private JsonObject config;
    private WebClientFactory webClientFactory;

    @BeforeEach
    public void setup(Vertx vertx) {
        config = new JsonObject();
        config.put("catalogueHost", "abcdefg");
        config.put("cataloguePort", 123);
        webClientFactory = new WebClientFactory(vertx, config);
    }

    @DisplayName("Test getWebClientFor method for Unknown serverType")
    @Test
    public void testGetWebClientFor(VertxTestContext vertxTestContext) {
        ServerType serverType = mock(ServerType.class);
        assertNull(webClientFactory.getWebClientFor(serverType));
        vertxTestContext.completeNow();
    }

    @DisplayName("Test getWebClientFor method for ServerType : FILE_SERVER")
    @Test
    public void testGetWebClientForFileServerType(VertxTestContext vertxTestContext) {
        ServerType serverTypeMock = ServerType.FILE_SERVER;
        var actual = webClientFactory.getWebClientFor(serverTypeMock);
        assertEquals(WebClientBase.class, actual.getClass());
        vertxTestContext.completeNow();
    }

    @DisplayName("Test getWebClient method for ServerType: RESOURCE_SERVER")
    @Test
    public void testGetWebClientForResourceServerType(VertxTestContext vertxTestContext) {
        ServerType serverType = ServerType.RESOURCE_SERVER;
        WebClient actual = webClientFactory.getWebClientFor(serverType);
        assertEquals(WebClientBase.class, actual.getClass());
        vertxTestContext.completeNow();
    }

}
