package iudx.file.server.cache;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testcontainers.junit.jupiter.Testcontainers;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.cache.cacheimpl.CacheType;
import iudx.file.server.database.postgres.PostgresService;

@Testcontainers
@ExtendWith({VertxExtension.class})
@TestMethodOrder(OrderAnnotation.class)
public class CacheServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(CacheServiceTest.class);
  static CacheService cacheService;
  static PostgresService pgService;

  static JsonObject testJson_0 = new JsonObject()
      .put("type", CacheType.REVOKED_CLIENT)
      .put("key", "revoked_client_id_0")
      .put("value", "2020-10-18T14:20:00Z");

  static JsonObject testJson_1 = new JsonObject()
      .put("type", CacheType.REVOKED_CLIENT)
      .put("key", "revoked_client_id_1")
      .put("value", "2020-10-19T14:20:00Z");

  @BeforeAll
  public static void setup(Vertx vertx, VertxTestContext testContext) {
    pgService = mock(PostgresService.class);
    cacheService = new CacheServiceImpl(pgService);
    testContext.completeNow();
  }


  @Test
  public void cachePutTest(Vertx vertx, VertxTestContext testContext) {
    cacheService.put(testJson_0, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("failed to insert in cache");;
      }
    });
  }

  @Test
  public void cachePutTest2(Vertx vertx, VertxTestContext testContext) {
    cacheService.put(testJson_1, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("failed to insert in cache");;
      }
    });
  }

  @Description("fail for no type present in json request")
  @Test
  public void failCachePutTestNoType(Vertx vertx, VertxTestContext testContext) {
    JsonObject json = testJson_0.copy();
    json.remove("type");
    cacheService.put(json, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("failed - inserted in cache for no type");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Description("fail for invalid type present in json request")
  @Test
  public void failCachePutTestInvalidType(Vertx vertx, VertxTestContext testContext) {
    JsonObject json = testJson_0.copy();
    json.put("type", "invalid_cache_type");
    cacheService.put(json, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("failed - inserted in cache for no type");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Description("fail for no key present in json request")
  @Test
  public void failCachePutTestNoKey(Vertx vertx, VertxTestContext testContext) {
    JsonObject json = testJson_0.copy();
    json.remove("key");
    cacheService.put(json, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("failed - inserted in cache for no key");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Description("fail for no value present in json request")
  @Test
  public void failCachePutTestNoValue(Vertx vertx, VertxTestContext testContext) {
    JsonObject json = testJson_0.copy();
    json.remove("value");
    cacheService.put(json, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("failed - inserted in cache for no value");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  public void getValueFromCache(Vertx vertx, VertxTestContext testContext) {
    JsonObject json = testJson_0.copy();
    json.remove("value");

    cacheService.put(testJson_0, handler -> {
    });

    cacheService.get(json, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        assertTrue(resultJson.containsKey("value"));
        assertEquals(testJson_0.getString("value"), resultJson.getString("value"));
        testContext.completeNow();
      } else {
        testContext.failNow("no value returned for known key value.");
      }
    });
  }


  @Test
  public void getValueFromCache2(Vertx vertx, VertxTestContext testContext) {
    JsonObject json = testJson_1.copy();
    json.remove("value");

    cacheService.put(testJson_1, handler -> {
    });

    cacheService.get(json, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        assertTrue(resultJson.containsKey("value"));
        assertEquals(testJson_1.getString("value"), resultJson.getString("value"));
        testContext.completeNow();
      } else {
        testContext.failNow("no value returned for known key value.");
      }
    });
  }

  @Description("fail -  get cahce for no type")
  @Test
  public void failGetValueFromCacheNoType(Vertx vertx, VertxTestContext testContext) {
    JsonObject json = testJson_1.copy();
    json.remove("type");

    cacheService.get(json, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("get operation succeeded for no type in request");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Description("fail -  get cahce for invalid(null)")
  @Test
  public void failGetValueFromCacheInvalidKey(Vertx vertx, VertxTestContext testContext) {
    JsonObject json = testJson_1.copy();
    json.remove("key");

    cacheService.get(json, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("get operation succeeded for invalid(null) in request");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Description("fail -  get cahce for key not in cache")
  @Test
  public void failGetValueFromCacheNoKey(Vertx vertx, VertxTestContext testContext) {
    JsonObject json = testJson_1.copy();
    json.put("key", "123");

    cacheService.get(json, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("get operation succeeded for key not in cache");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Description("refresh cache passing key and value")
  @Test
  public void refreshCacheTest(Vertx vertx, VertxTestContext testContext) {
    cacheService.refresh(testJson_0, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
        JsonObject json = testJson_0.copy();
        json.remove("value");

        cacheService.get(json, getHandler -> {
          if (getHandler.succeeded()) {
            assertEquals(testJson_0.getString("value"), getHandler.result().getString("value"));
            testContext.completeNow();
          } else {
            testContext.failNow("fail to fetch value for key");
          }
        });
      } else {
        testContext.failNow("fail to refresh cache");
      }
    });
  }


  @Description("refresh cache without passing key and value.")
  @Test
  public void refreshCacheTest_1(Vertx vertx, VertxTestContext testContext) {
    // prepare request json for CacheServiceImpl.refresh()
    JsonObject refresh_JSON = testJson_0.copy();
    refresh_JSON.remove("key");
    refresh_JSON.remove("value");

    // prepare mocked response from database/postgres service.
    JsonObject pgResponse = new JsonObject();
    JsonArray responseArray = new JsonArray();
    pgResponse.put("_id", "key_from_postgres").put("expiry", "2020-10-19T14:20:00Z");
    responseArray.add(pgResponse);


    // prepare mock AsyncResult(Handler) to be used as argument PostgresService.executreQuery()
    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(new JsonObject().put("result", responseArray));


    // connect PostgresService.executeQuery() to AsyncResult(Handler)
    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(pgService).executeQuery(any(String.class), any());

    // Test
    // call cache refresh
    cacheService.refresh(refresh_JSON, handler -> {
      if (handler.succeeded()) {
        JsonObject json = testJson_0.copy();
        json.remove("value");
        json.put("key", "key_from_postgres");
        // verify
        // cross check by calling cache get
        cacheService.get(json, getHandler -> {
          if (getHandler.succeeded()) {
            //verify
            assertEquals("2020-10-19T14:20:00Z", getHandler.result().getString("value"));
            // executeQuery() will be called 2 times once from constructor and once from refresh()
            verify(pgService, times(2)).executeQuery(any(String.class), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail to fetch value for key");
          }
        });
      } else {
        testContext.failNow("fail to refresh cache");
      }
    });
    testContext.completeNow();
  }
  @DisplayName("Test refresh method for illegal argument: request")
  @Test
  public void testRefreshForInvalidRequest(VertxTestContext vertxTestContext)
  {
    PostgresService postgresServiceMock = mock(PostgresService.class);
    CacheServiceImpl cacheServiceImpl = new CacheServiceImpl(postgresServiceMock);
    JsonObject requestMock = mock(JsonObject.class);
    when(requestMock.getString(anyString())).thenReturn("Dummy String");

    cacheServiceImpl.refresh(requestMock,handler -> {
      if(handler.succeeded())
      {
        vertxTestContext.failNow("Succeeded for invalid request");
      }
      else
      {
        vertxTestContext.completeNow();
      }
    });
  }

}
