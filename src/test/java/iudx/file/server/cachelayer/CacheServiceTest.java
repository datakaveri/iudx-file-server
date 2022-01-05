package iudx.file.server.cachelayer;

import static iudx.file.server.common.Constants.DATA_BROKER_SERVICE_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.DeploymentOptions;
import iudx.file.server.databroker.DataBrokerService;
import iudx.file.server.databroker.DataBrokerVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.configuration.Configuration;

@ExtendWith(VertxExtension.class)
public class CacheServiceTest {

  private static CacheServiceImpl cacheService;
  private static DataBrokerService dataBrokerService;
  private static Configuration appConfig;

  private static final Logger logger = LogManager.getLogger(CacheServiceTest.class);

  @BeforeAll
  @DisplayName("Deploy verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    appConfig = new Configuration();
    JsonObject cacheConfig = appConfig.configLoader(5, vertx);

    JsonObject databrokerConfig = appConfig.configLoader(1, vertx);
    vertx.deployVerticle(DataBrokerVerticle.class.getName(),
            new DeploymentOptions()
                    .setInstances(1)
                    .setConfig(databrokerConfig),
            databrokerDeployer -> {
              if (databrokerDeployer.succeeded()) {
                dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
                cacheService = new CacheServiceImpl(vertx, dataBrokerService);

                testContext.completeNow();
              } else {
                testContext.failNow("fail to deploy DataBroker Verticle in Cache Layer Test");
              }
            });
  }

  @Test
  @DisplayName("successful population of cache")
  public void populateCacheTest(VertxTestContext testContext) {

    JsonObject jsonObject = new JsonObject().put("844e251b-574b-46e6-9247-f76f1f70a637","2020-12-22T09:18");

    if(cacheService.tokenInvalidationCache.size() > 0) {
      testContext.completeNow();
    } else {
      cacheService.populateCache(jsonObject);
      if(cacheService.tokenInvalidationCache.size() > 0) {
        testContext.completeNow();
      } else {
        testContext.failNow("failed to populate invalidation cache");
      }
    }
  }

  @Test
  @DisplayName("successful get from cache")
  public void getFromCacheTest(VertxTestContext testContext) {

    String _id = "844e251b-574b-46e6-9247-f76f1f70a637";
    String expected = "2020-12-22T09:18";

    cacheService.getInvalidationTimeFromCache(_id, cacheResultHandler -> {
      if(cacheResultHandler.succeeded()) {
        String actual = cacheResultHandler.result();
        assertEquals(expected, actual);
        testContext.completeNow();
      } else {
        logger.debug(cacheResultHandler.cause());
        testContext.failNow("failed to get value from cache");
      }
    });
  }
}
