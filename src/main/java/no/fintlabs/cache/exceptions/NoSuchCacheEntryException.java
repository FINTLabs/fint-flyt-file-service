package no.fintlabs.cache.exceptions;

public class NoSuchCacheEntryException extends RuntimeException {

    public NoSuchCacheEntryException(String key) {
        super(String.format("No cache entry with key='%s'", key));
    }

}
