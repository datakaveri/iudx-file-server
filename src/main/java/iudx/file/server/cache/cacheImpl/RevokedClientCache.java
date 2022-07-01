package iudx.file.server.cache.cacheImpl;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.file.server.database.postgres.PostgresConstants;
import iudx.file.server.database.postgres.PostgresService;

public class RevokedClientCache implements IudxCache {

  private static final Logger LOGGER = LogManager.getLogger(RevokedClientCache.class);
  private final static CacheType cacheType = CacheType.REVOKED_CLIENT;

  private final Cache<String, String> cache =
      CacheBuilder.newBuilder()
          .maximumSize(5000)
          .expireAfterWrite(1L, TimeUnit.DAYS)
          .build();

  private PostgresService pgService;

  public RevokedClientCache(PostgresService postgresService) {
    this.pgService = postgresService;
    refreshCache();
  }

  @Override
  public void put(String key, String value) {
    cache.put(key, value);
  }

  @Override
  public String get(String key) {
    return cache.getIfPresent(key);
  }

  @Override
  public void refreshCache() {
    LOGGER.debug(cacheType + " refreshCache() called");
    String query = PostgresConstants.SELECT_REVOKE_TOKEN_SQL;
    pgService.executeQuery(query, handler -> {
      if (handler.succeeded()) {
        JsonArray clientIdArray = handler.result().getJsonArray("result");

        clientIdArray.forEach(e -> {
          JsonObject clientInfo = (JsonObject) e;
          String key = clientInfo.getString("_id");
          String value = clientInfo.getString("expiry");
          this.cache.put(key, value);
          LOGGER.debug("cache size : " + this.cache.size());
        });

      }
    });
  }



}
