package no.fintlabs.cache;

public interface FintCacheManager {

    <K, V> FintCache<K, V> createCache(String alias, Class<K> keyClass, Class<V> valueClass);

    <K, V> FintCache<K, V> createCache(String alias, Class<K> keyClass, Class<V> valueClass, FintCacheOptions cacheOptions);

    <K, V> FintCache<K, V> getCache(String alias, Class<K> keyClass, Class<V> valueClass);

    <K, V> void removeCache(String alias);

}
