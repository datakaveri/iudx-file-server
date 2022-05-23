package iudx.file.server.database.postgres;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.file.server.configuration.Configuration;

@ExtendWith(VertxExtension.class)
public class PostgresServiceImplTest {

  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImplTest.class);
  private static PostgresServiceImpl postgresServiceImpl;
  private static PostgreSQLContainer<?> postgresContainer;
  // @TODO : change configs to get image version, image version and version used by IUDX should be
  // same.
  public static String CONTAINER = "postgres:12.11";
  public static String database = "iudx";
  private static Configuration config;
  private static JsonObject dbConfig;

  @BeforeAll
  static void setup(Vertx vertx, VertxTestContext testContext) {

    config = new Configuration();
    dbConfig = config.configLoader(2, vertx);

    dbConfig.put("databaseIp", "localhost");
    dbConfig.put("databasePort", 5432);
    dbConfig.put("databaseName", database);
    dbConfig.put("databaseUserName", "iudx_user");
    dbConfig.put("databasePassword", "pg@postgres.dk");
    dbConfig.put("poolSize", 25);

    postgresContainer = new PostgreSQLContainer<>(CONTAINER).withInitScript("pg_test_schema.sql");

    postgresContainer.withUsername(dbConfig.getString("databaseUserName"));
    postgresContainer.withPassword(dbConfig.getString("databasePassword"));
    postgresContainer.withDatabaseName(dbConfig.getString("databaseName"));
    postgresContainer.withExposedPorts(dbConfig.getInteger("databasePort"));


    postgresContainer.start();
    if (postgresContainer.isRunning()) {
      dbConfig.put("databasePort", postgresContainer.getFirstMappedPort());

      PgConnectOptions connectOptions =
          new PgConnectOptions()
              .setPort(dbConfig.getInteger("databasePort"))
              .setHost(dbConfig.getString("databaseIp"))
              .setDatabase(dbConfig.getString("databaseName"))
              .setUser(dbConfig.getString("databaseUserName"))
              .setPassword(dbConfig.getString("databasePassword"))
              .setReconnectAttempts(2)
              .setReconnectInterval(1000);


      PoolOptions poolOptions = new PoolOptions().setMaxSize(dbConfig.getInteger("poolSize"));
      PgPool pool = PgPool.pool(vertx, connectOptions, poolOptions);

      postgresServiceImpl = new PostgresServiceImpl(pool);
      testContext.completeNow();
    }
  }

  @Test
  @Description("test whether able to fetch all records correctly ot not.")
  public void testSuccess(Vertx vertx, VertxTestContext testContext) {
    postgresServiceImpl.executeQuery(PostgresConstants.SELECT_REVOKE_TOKEN_SQL, handler -> {
      if (handler.succeeded()) {
        JsonObject queryResponse = handler.result();
        assertTrue(queryResponse.containsKey("result"));
        assertEquals(4, handler.result().getJsonArray("result").size());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Description("check failure query execution [non existing table]")
  public void testFailureNoTable(Vertx vertx, VertxTestContext testContext) {
    postgresServiceImpl.executeQuery("select * from revoked_tokens123", handler -> {
      if (handler.succeeded()) {
        testContext.failNow("passed for failing condition");;
      } else {

        JsonObject failureCause = new JsonObject(handler.cause().getMessage());
        assertTrue(failureCause.containsKey("type"));
        assertTrue(failureCause.containsKey("status"));
        assertTrue(failureCause.containsKey("detail"));
        assertNotNull(failureCause.getValue("detail"));
        assertEquals(400, failureCause.getInteger("status"));
        assertEquals("urn:dx:rs:dberror", failureCause.getString("type"));

        testContext.completeNow();
      }
    });
  }

  @Test
  @Description("check failure query execution [Invalid query]")
  public void testFailureInvalidQuery(Vertx vertx, VertxTestContext testContext) {
    postgresServiceImpl.executeQuery("selectdsa * from revoked_tokens123", handler -> {
      if (handler.succeeded()) {
        testContext.failNow("passed for failing condition");;
      } else {

        JsonObject failureCause = new JsonObject(handler.cause().getMessage());
        assertTrue(failureCause.containsKey("type"));
        assertTrue(failureCause.containsKey("status"));
        assertTrue(failureCause.containsKey("detail"));
        assertNotNull(failureCause.getValue("detail"));
        assertEquals(400, failureCause.getInteger("status"));
        assertEquals("urn:dx:rs:dberror", failureCause.getString("type"));

        testContext.completeNow();
      }
    });
  }

  @Test
  @Description("test whether able to fetch all records correctly or not through prepared query.")
  public void testSuccessPreparedQuery(Vertx vertx, VertxTestContext testContext) {
    JsonObject queryParam = new JsonObject();
    queryParam.put("_id", "0ed019fe-be38-4903-8f0f-5285d2985780");
    postgresServiceImpl.executePreparedQuery("select * from revoked_tokens where _id=$1",
        queryParam, handler -> {
          if (handler.succeeded()) {
            JsonObject queryResponse = handler.result();
            assertTrue(queryResponse.containsKey("result"));
            assertEquals(1, handler.result().getJsonArray("result").size());

            JsonObject result = handler.result().getJsonArray("result").getJsonObject(0);
            assertEquals("0ed019fe-be38-4903-8f0f-5285d2985780", result.getString("_id"));

            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Description("test whether able to fetch all records correctly or not through prepared query.[invalid query]")
  public void testFailurePreparedQuery(Vertx vertx, VertxTestContext testContext) {
    JsonObject queryParam = new JsonObject();
    queryParam.put("_id", "0ed019fe-be38-4903-8f0f-5285d2985780");
    postgresServiceImpl.executePreparedQuery("select * from revoked_tokens where _id=?", queryParam,
        handler -> {
          if (handler.succeeded()) {
            testContext.failNow("passed for failure case");
          } else {
            JsonObject failureCause = new JsonObject(handler.cause().getMessage());
            assertTrue(failureCause.containsKey("type"));
            assertTrue(failureCause.containsKey("status"));
            assertTrue(failureCause.containsKey("detail"));
            assertNotNull(failureCause.getValue("detail"));
            assertEquals(400, failureCause.getInteger("status"));
            assertEquals("urn:dx:rs:dberror", failureCause.getString("type"));

            testContext.completeNow();
          }
        });
  }

  @AfterAll
  public static void destroy(Vertx vertx, VertxTestContext testContext) {
    postgresContainer.close();
    testContext.completeNow();
  }
}
