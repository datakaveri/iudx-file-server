package iudx.file.server.cache.cacheimpl;

public interface IudxCache {

  void put(String key, String value);

  String get(String key);

  void refreshCache();
}
