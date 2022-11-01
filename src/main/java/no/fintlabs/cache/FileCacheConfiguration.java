package no.fintlabs.cache;

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
