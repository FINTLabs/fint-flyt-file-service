package no.novari.flyt.files.application

import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.cache.FintCache
import no.novari.cache.exceptions.NoSuchCacheEntryException
import no.novari.flyt.files.domain.FilePayload
import no.novari.flyt.files.domain.exception.FileNotFoundException
import no.novari.flyt.files.infrastructure.storage.FileRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FileService(
    private val fileCache: FintCache<UUID, FilePayload>,
    private val fileRepository: FileRepository,
) {
    private val log = KotlinLogging.logger {}

    fun findById(fileId: UUID): FilePayload {
        val fileFromCache =
            try {
                fileCache.get(fileId)
            } catch (_: NoSuchCacheEntryException) {
                null
            }
        val fileFromStorage = fileFromCache ?: fileRepository.findById(fileId)
        return fileFromStorage ?: throw FileNotFoundException(fileId)
    }

    fun put(
        fileId: UUID,
        file: FilePayload,
    ): UUID {
        fileCache.put(fileId, file)

        return try {
            fileRepository.putFile(fileId, file)
        } catch (exception: Exception) {
            fileCache.remove(fileId)
            throw exception
        }
    }

    fun delete(fileIds: List<UUID>) {
        if (fileIds.isEmpty()) {
            log.atInfo {
                message = "List of fileIds is empty"
            }
            return
        }

        fileCache.remove(fileIds)
        fileRepository.deleteFiles(fileIds)
    }

    fun deleteFilesOlderThan(days: Int): Int {
        return fileRepository.deleteFilesOlderThan(days)
    }
}
