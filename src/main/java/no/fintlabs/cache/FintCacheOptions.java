package no.fintlabs.cache;

import lombok.Builder;

import java.time.Duration;

@Builder
public class FintCacheOptions {

    public final Duration timeToLive;
    public final Long heapSize;

}
