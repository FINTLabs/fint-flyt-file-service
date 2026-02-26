package no.novari.flyt.files.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class FileCleanupService(
    @param:Value("\${novari.flyt.file-service.time-to-keep-files-in-days:180}")
    private val timeToKeepFilesInDays: Int,
    private val fileService: FileService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(initialDelay = 30000, fixedDelay = 86400000)
    fun cleanup() {
        log.atInfo {
            message = "Cleaning up files older than {} days"
            arguments = arrayOf(timeToKeepFilesInDays)
        }
        val numberOfDeletedFiles = fileService.deleteFilesOlderThan(timeToKeepFilesInDays)
        log.atInfo {
            message = "Deleted {} files during cleanup"
            arguments = arrayOf(numberOfDeletedFiles)
        }
    }
}
