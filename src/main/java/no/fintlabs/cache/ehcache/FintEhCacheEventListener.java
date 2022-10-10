package no.fintlabs.cache.ehcache;

import no.fintlabs.cache.FintCacheEvent;
import no.fintlabs.cache.FintCacheEventListener;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;

public abstract class FintEhCacheEventListener<K, V> implements FintCacheEventListener<K, V>, CacheEventListener<K, V> {

    @Override
    public void onEvent(CacheEvent<K, V> event) {
        this.onEvent(this.map(event));
    }

    private FintCacheEvent<K, V> map(CacheEvent<K, V> event) {
        return new FintCacheEvent<>(
                this.map(event.getType()),
                event.getKey(),
                event.getOldValue(),
                event.getNewValue()
        );
    }

    private FintCacheEvent.EventType map(EventType type) {
        return switch (type) {
            case CREATED -> FintCacheEvent.EventType.CREATED;
            case UPDATED -> FintCacheEvent.EventType.UPDATED;
            case REMOVED -> FintCacheEvent.EventType.REMOVED;
            case EXPIRED -> FintCacheEvent.EventType.EXPIRED;
            case EVICTED -> FintCacheEvent.EventType.EVICTED;
        };
    }
}
