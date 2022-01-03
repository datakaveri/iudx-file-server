package iudx.file.server.cachelayer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.file.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static iudx.file.server.databroker.util.Constants.CACHE_TIMEOUT_AMOUNT;

public class CacheServiceImpl implements CacheService {

  private static final Logger LOGGER = LogManager.getLogger(CacheServiceImpl.class);

  final DataBrokerService dataBrokerService;

  public final Cache<String, String> tokenInvalidationCache =
          CacheBuilder.newBuilder()
                  .maximumSize(1000)
                  .expireAfterWrite(CACHE_TIMEOUT_AMOUNT, TimeUnit.HOURS)
                  .build();

  CacheServiceImpl(Vertx vertx, final DataBrokerService dataBrokerService) {

    this.dataBrokerService = dataBrokerService;

    /* populate cache on startup */
    populateCacheFromDB();

    /* periodically pull invalidation data to update cache */
    vertx.setPeriodic(TimeUnit.HOURS.toMillis(6), timerHandler -> populateCacheFromDB());
  }

  private void populateCacheFromDB() {

    dataBrokerService.getInvalidationDataFromDB(invalidationResultHandler -> {
      if(invalidationResultHandler.succeeded()) {
        JsonObject jsonResult = invalidationResultHandler.result();
        populateCache(jsonResult);
      } else {
        LOGGER.info(invalidationResultHandler.cause());
      }
    });
  }

  @Override
  public CacheService populateCache(JsonObject invalidationResult) {

    LOGGER.info("populate cache is called");

    Set<String> keySet = invalidationResult.getMap().keySet();
    keySet.forEach((key) -> {
      String modifiedAt = invalidationResult.getString(key);
      tokenInvalidationCache.put(key,modifiedAt);
    });
    return this;
  }

  @Override
  public CacheService getInvalidationTimeFromCache(String id, Handler<AsyncResult<String>> handler) {
    String modified_at = tokenInvalidationCache.getIfPresent(id);

    if(modified_at != null && !modified_at.isBlank()) {
      handler.handle(Future.succeededFuture(modified_at));
    } else {
      populateCacheFromDB();
      modified_at = tokenInvalidationCache.getIfPresent(id);
      if(modified_at != null && !modified_at.isBlank()) {
        handler.handle(Future.succeededFuture(modified_at));
      } else {
        handler.handle(Future.failedFuture("id not in cache"));
      }
    }

    return this;
  }
}
