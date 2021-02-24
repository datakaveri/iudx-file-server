package iudx.file.server.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.UUID;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import iudx.file.server.configuration.Configuration;
import iudx.file.server.service.impl.PGTokenStoreImpl;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.pgclient.PgPool;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class TokenStoreTesting {
  
  @Rule
  @Container
  public static PostgreSQLContainer<?> container = new PostgreSQLContainer<>().withInitScript("schema.sql");

  private static Configuration appConfig;
  private static TokenStore tokenStore;
  private static String authToken;
  private static String fileToken;
  private static String serverId;


  @BeforeAll
  @DisplayName("Deploy a verticle")
  static void init(Vertx vertx, io.vertx.reactivex.core.Vertx vertx2,
      VertxTestContext testContext) {
    appConfig = new Configuration();
    JsonObject tokenStoreConfigs = appConfig.configLoader(0, vertx2);
    
    PgConnectOptions connectOptions = new PgConnectOptions()
        .setPort(container.getMappedPort(5432))
        .setHost(container.getContainerIpAddress())
        .setDatabase(container.getDatabaseName())
        .setUser(container.getUsername())
        .setPassword(container.getPassword())
        .addProperty("search_path", "public");
    
    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

    PgPool client = PgPool.pool(connectOptions, poolOptions);

    authToken = UUID.randomUUID().toString();
    fileToken = UUID.randomUUID().toString();
    serverId = UUID.randomUUID().toString();

    tokenStore = new PGTokenStoreImpl(client);
    testContext.completeNow();
  }

  @Test
  @Order(1)
  @DisplayName("Put a token in token store")
  void testPutTokenInStore(VertxTestContext testContext) {
    tokenStore.put(authToken, fileToken, serverId).onComplete(handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        System.out.println(result);
        assertTrue(result.containsKey("fileServerToken"));
        assertTrue(result.containsKey("validity"));
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(2)
  @DisplayName("get details of a token in token store")
  void testGetToken4rmStore(VertxTestContext testContext) {
    tokenStore.getTokenDetails(authToken).onComplete(handler -> {
      if (handler.succeeded()) {
        Map<String, String> result = handler.result();
        assertTrue(result.containsKey("file_token"));
        assertTrue(result.containsKey("validity_date"));
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }
  
  @Test
  @Order(3)
  @DisplayName("get details of a token in token store (invalid token)")
  void testGetToken4rmStoreInvalidAuthToken(VertxTestContext testContext) {
    tokenStore.getTokenDetails(UUID.randomUUID().toString()).onComplete(handler -> {
      if (!handler.succeeded()) {
        String cause=handler.cause().getMessage();
        assertEquals(cause,"no token exist.");
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(4)
  @DisplayName("delete token in token store")
  void testDeleteToken4rmStore(VertxTestContext testContext) {
    tokenStore.delete(fileToken).onComplete(handler -> {
      if (handler.succeeded()) {
        Boolean result = handler.result();
        assertTrue(result);
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

}

