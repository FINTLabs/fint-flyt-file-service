package no.novari.flyt.files.infrastructure.storage

import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.files.domain.FilePayload
import no.novari.flyt.files.domain.exception.FileStorageException
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class FileRepository(
    private val blobStorageAdapter: BlobStorageAdapter,
) {
    private val log = KotlinLogging.logger {}

    fun putFile(
        fileId: UUID,
        file: FilePayload,
    ): UUID {
        return try {
            val uploadedFileId = blobStorageAdapter.uploadFile(fileId, file)
            logSuccessfulAction(fileId, "uploaded")
            uploadedFileId
        } catch (exception: Exception) {
            log.atError {
                message = "Could not upload file with fileId={}"
                arguments = arrayOf(fileId)
                cause = exception
            }
            throw FileStorageException("Could not upload file", exception)
        }
    }

    fun findById(fileId: UUID): FilePayload? {
        return try {
            val filePayload = blobStorageAdapter.downloadFile(fileId)
            if (filePayload != null) {
                logSuccessfulAction(fileId, "found")
            } else {
                logUnsuccessfulAction(fileId, "find")
            }
            filePayload
        } catch (exception: Exception) {
            log.atError {
                message = "Could not download file with fileId={}"
                arguments = arrayOf(fileId)
                cause = exception
            }
            throw FileStorageException("Could not download file", exception)
        }
    }

    fun deleteFiles(fileIds: List<UUID>) {
        try {
            blobStorageAdapter.deleteFilesByIds(fileIds)
            fileIds.forEach { fileId -> logSuccessfulAction(fileId, "deleted") }
        } catch (exception: Exception) {
            log.atError {
                message = "Could not delete files with fileIds={}"
                arguments = arrayOf(fileIds)
                cause = exception
            }
            throw FileStorageException("Could not delete files", exception)
        }
    }

    fun deleteFilesOlderThan(days: Int): Int {
        try {
            val deletedFiles = blobStorageAdapter.deleteFilesOlderThanDays(days)
            deletedFiles.forEach { deletedFile ->
                log.atInfo {
                    message = "deleted file with name {}, timestamp {}"
                    arguments = arrayOf(deletedFile.name, deletedFile.deletedAt)
                }
            }
            return deletedFiles.size
        } catch (exception: Exception) {
            log.atError {
                message = "Could not delete files older than {} days"
                arguments = arrayOf(days)
                cause = exception
            }
            throw FileStorageException("Could not delete old files", exception)
        }
    }

    private fun logSuccessfulAction(
        fileId: UUID,
        performedAction: String,
    ) {
        log.atInfo {
            message = "Successfully {} File{fileId={}} in file storage"
            arguments = arrayOf(performedAction, fileId)
        }
    }

    private fun logUnsuccessfulAction(
        fileId: UUID,
        actionToBePerformed: String,
    ) {
        log.atWarn {
            message = "Could not {} File{fileId={}} in file storage"
            arguments = arrayOf(actionToBePerformed, fileId)
        }
    }
}
