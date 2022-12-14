package no.fintlabs;

import no.fintlabs.cache.FintCache;
import no.fintlabs.cache.FintCacheManager;
import no.fintlabs.model.File;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class FileCacheConfiguration {
    @Bean
    FintCache<UUID, File> fileCache(FintCacheManager fintCacheManager) {
        return fintCacheManager.createCache(
                "file",
                UUID.class,
                File.class
        );
    }
}
