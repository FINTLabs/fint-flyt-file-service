package no.novari.flyt.config

import no.novari.cache.FintCache
import no.novari.cache.FintCacheManager
import no.novari.flyt.files.domain.FilePayload
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID

@Configuration
class FileCacheConfiguration {
    @Bean
    fun fileCache(fintCacheManager: FintCacheManager): FintCache<UUID, FilePayload> {
        return fintCacheManager.createCache(
            "file",
            UUID::class.java,
            FilePayload::class.java,
        )
    }
}
