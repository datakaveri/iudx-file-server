package iudx.file.server.cache.cacheImpl;

public interface IudxCache {

    void put(String key, String value);

    String get(String key);

    void refreshCache();
}
