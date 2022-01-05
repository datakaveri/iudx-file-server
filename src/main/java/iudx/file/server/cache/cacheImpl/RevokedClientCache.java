package iudx.file.server.cache.cacheImpl;

import java.util.concurrent.TimeUnit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import iudx.file.server.database.postgres.PostgresService;

public class RevokedClientCache implements IudxCache {

  private final Cache<String, String> revokedClientCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(3L, TimeUnit.HOURS)
          .build();

  private PostgresService pgService;

  public RevokedClientCache(PostgresService postgresService) {
    this.pgService=pgService;
  }

  @Override
  public void put(String key, String value) {
    revokedClientCache.put(key, value);
  }

  @Override
  public String get(String key) {
    return revokedClientCache.getIfPresent(key);
  }

  @Override
  public void refreshCache() {
    // TODO Auto-generated method stub

  }



}
