package no.fintlabs.cache;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FintCacheInfo {
    private final String alias;
    private final long numberOfEntries;
    private final long numberOfDistinctEntries;
}
