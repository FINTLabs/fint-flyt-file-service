package no.novari.flyt.file;

import no.novari.cache.FintCache;
import no.novari.cache.FintCacheManager;
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
