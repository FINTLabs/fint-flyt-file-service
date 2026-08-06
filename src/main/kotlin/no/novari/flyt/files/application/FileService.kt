package no.novari.flyt.files.application

import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.files.domain.FileDownload
import no.novari.flyt.files.domain.FileMetadata
import no.novari.flyt.files.domain.FilePayload
import no.novari.flyt.files.domain.exception.FileNotFoundException
import no.novari.flyt.files.infrastructure.storage.FileRepository
import org.springframework.stereotype.Service
import java.text.Normalizer
import java.util.UUID

@Service
class FileService(
    private val fileRepository: FileRepository,
) {
    private val log = KotlinLogging.logger {}

    fun openDownload(fileId: UUID): FileDownload {
        val fileDownload = fileRepository.openDownload(fileId) ?: throw FileNotFoundException(fileId)
        val normalizedMetadata = normalizeFileName(fileDownload.metadata)

        log.atDebug {
            message = "Resolved file metadata for fileId={} normalizedNameChanged={}"
            arguments =
                arrayOf(
                    fileId,
                    fileDownload.metadata.name != normalizedMetadata.name,
                )
        }

        return FileDownload(normalizedMetadata, fileDownload.contents)
    }

    fun put(
        fileId: UUID,
        file: FilePayload,
    ): UUID {
        val normalizedFile = normalizeFileName(file)

        log.atDebug {
            message = "Preparing file upload for fileId={} normalizedNameChanged={}"
            arguments =
                arrayOf(
                    fileId,
                    file.name != normalizedFile.name,
                )
        }

        return fileRepository.putFile(fileId, normalizedFile)
    }

    fun delete(fileIds: List<UUID>) {
        if (fileIds.isEmpty()) {
            log.atInfo {
                message = "List of fileIds is empty"
            }
            return
        }

        fileRepository.deleteFiles(fileIds)
    }

    fun deleteFilesOlderThan(days: Int): Int {
        return fileRepository.deleteFilesOlderThan(days)
    }

    private fun normalizeFileName(file: FileMetadata): FileMetadata {
        val normalizedName = normalizeFileName(file.name)

        return if (normalizedName == file.name) {
            file
        } else {
            file.copy(name = normalizedName)
        }
    }

    private fun normalizeFileName(file: FilePayload): FilePayload {
        val normalizedName = normalizeFileName(file.name)

        return if (normalizedName == file.name) {
            file
        } else {
            file.copy(name = normalizedName)
        }
    }

    private fun normalizeFileName(fileName: String): String {
        return fileName
            .trim()
            .replace(WHITESPACE_BEFORE_EXTENSION_REGEX, "$1")
            .replace(TYPOGRAPHIC_DASHES_REGEX, "-")
            .let { Normalizer.normalize(it, Normalizer.Form.NFC) }
    }

    companion object {
        private val WHITESPACE_BEFORE_EXTENSION_REGEX = Regex("""\s+(\.[^.\s]+(?:\.[^.\s]+)*)$""")
        private val TYPOGRAPHIC_DASHES_REGEX = Regex("[\u2013\u2014]")
    }
}
