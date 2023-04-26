package iudx.file.server.cache.cacheimpl;

/**
 * CacheType.
 *
 * <h1>iudx CacheType </h1>
 */
public enum CacheType {
  REVOKED_CLIENT("revoked_client");

  String cacheName;

  CacheType(String name) {
    this.cacheName = name;
  }
}
