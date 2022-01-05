package iudx.file.server.cache;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.file.server.cache.cacheImpl.CacheType;
import iudx.file.server.cache.cacheImpl.IudxCache;
import iudx.file.server.cache.cacheImpl.RevokedClientCache;
import iudx.file.server.database.postgres.PostgresService;

public class CacheServiceImpl implements CacheService {

  private IudxCache revokedClientCache;
  private PostgresService postgresService;

  public CacheServiceImpl(PostgresService pgService) {
    this.postgresService = pgService;

    revokedClientCache = new RevokedClientCache(postgresService);
  }

  @Override
  public CacheService get(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    IudxCache cache = getCache(request);
    if (cache != null) {
      String value = cache.get(request.getString("key"));
      if (value != null) {
        JsonObject json = new JsonObject();
        json.put("value", value);
        handler.handle(Future.succeededFuture(json));
      } else {
        handler.handle(Future.failedFuture("No entry for given key"));
      }
    } else {
      handler.handle(Future.failedFuture("No cache defined for given type"));
    }

    return this;
  }

  @Override
  public CacheService put(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    IudxCache cache = getCache(request);
    if (cache != null) {
      cache.put(request.getString("key"), request.getString("value"));
      handler.handle(Future.succeededFuture());
    } else {
      handler.handle(Future.failedFuture("failed to put value in cache"));
    }
    return this;
  }

  @Override
  public CacheService refresh(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    IudxCache cache = getCache(request);
    cache.refreshCache();
    handler.handle(Future.succeededFuture());
    return this;
  }

  private IudxCache getCache(JsonObject json) {
    CacheType cacheType = CacheType.valueOf(json.getString("type"));
    IudxCache cache = null;
    switch (cacheType) {
      case REVOKED_CLIENT: {
        cache = revokedClientCache;
        break;
      }
      default: {
        // log and return false;
      }
    }

    return cache;
  }

}
