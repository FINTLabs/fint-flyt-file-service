package no.fintlabs.cache.ehcache;

import lombok.Getter;
import no.fintlabs.cache.FintCache;
import no.fintlabs.cache.FintCacheEventListener;
import org.ehcache.Cache;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FintEhCache<K, V> implements FintCache<K, V> {

    @Getter
    private final String alias;
    private final Cache<K, V> cache;

    public FintEhCache(String alias, Cache<K, V> cache) {
        this.alias = alias;
        this.cache = cache;
    }

    @Override
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    @Override
    public Optional<V> getOptional(K key) {
        return Optional.ofNullable(this.cache.get(key));
    }

    @Override
    public List<V> get(Collection<K> keys) {
        return new ArrayList<>(this.cache.getAll(new HashSet<>(keys)).values());
    }

    @Override
    public List<V> getAll() {
        return StreamSupport.stream(this.cache.spliterator(), false)
                .map(Cache.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public void put(K key, V value) {
        this.cache.put(key, value);
    }

    @Override
    public void put(Map<? extends K, ? extends V> entries) {
        this.cache.putAll(entries);
    }

    @Override
    public void remove(K key) {
        this.cache.remove(key);
    }

    @Override
    public void remove(Collection<K> keys) {
        this.cache.removeAll(new HashSet<>(keys));
    }

    @Override
    public void clear() {
        this.cache.clear();
    }

    @Override
    public void addEventListener(FintCacheEventListener<K, V> listener) {
        this.cache
                .getRuntimeConfiguration()
                .registerCacheEventListener(
                        (FintEhCacheEventListener<K, V>) listener,
                        EventOrdering.ORDERED,
                        EventFiring.SYNCHRONOUS,
                        Arrays.stream(EventType.values()).collect(Collectors.toSet())
                );
    }

    @Override
    public void removeEventListener(FintCacheEventListener<K, V> listener) {
        this.cache
                .getRuntimeConfiguration()
                .deregisterCacheEventListener((FintEhCacheEventListener<K, V>) listener);
    }

    @Override
    public String toString() {
        return String.format("FintEhCache{alias='%s', entries=%d}", this.getAlias(), this.getNumberOfEntries());
    }


}
