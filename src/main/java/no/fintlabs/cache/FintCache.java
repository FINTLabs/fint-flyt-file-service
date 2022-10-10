package no.fintlabs.cache;

import no.fintlabs.cache.exceptions.NoSuchCacheEntryException;

import java.util.*;
import java.util.stream.Collectors;

public interface FintCache<K, V> {

    String getAlias();

    boolean containsKey(K key);

    default V get(K key) {
        return this.getOptional(key).orElseThrow(() -> new NoSuchCacheEntryException(key.toString()));
    }

    Optional<V> getOptional(K key);

    List<V> get(Collection<K> keys);

    List<V> getAll();

    default List<V> getAllDistinct() {
        return this.getAll().stream().distinct().collect(Collectors.toList());
    }

    void put(K key, V value);

    default void put(Collection<K> keys, V value) {
        Map<K, V> map = keys.stream()
                .map(key -> new AbstractMap.SimpleEntry<>(key, value))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        this.put(map);
    }

    void put(Map<? extends K, ? extends V> entries);

    void remove(K key);

    void remove(Collection<K> keys);

    void clear();

    default long getNumberOfEntries() {
        return this.getAll().size();
    }

    default long getNumberOfDistinctValues() {
        return this.getAllDistinct().size();
    }

    void addEventListener(FintCacheEventListener<K, V> listener);

    void removeEventListener(FintCacheEventListener<K, V> listener);

}
