package no.novari.flyt.files.application

import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.cache.FintCache
import no.novari.cache.exceptions.NoSuchCacheEntryException
import no.novari.flyt.files.domain.FilePayload
import no.novari.flyt.files.domain.exception.FileNotFoundException
import no.novari.flyt.files.infrastructure.storage.FileRepository
import org.springframework.stereotype.Service
import java.text.Normalizer
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
        val normalizedFile = fileFromStorage?.let(::normalizeFileName) ?: throw FileNotFoundException(fileId)

        log.atDebug {
            message = "Resolved file name for fileId={} raw={} normalized={}"
            arguments =
                arrayOf(
                    fileId,
                    describeFileName(fileFromStorage.name),
                    describeFileName(normalizedFile.name),
                )
        }

        return normalizedFile
    }

    fun put(
        fileId: UUID,
        file: FilePayload,
    ): UUID {
        val normalizedFile = normalizeFileName(file)

        log.atDebug {
            message = "Preparing file upload for fileId={} raw={} normalized={}"
            arguments =
                arrayOf(
                    fileId,
                    describeFileName(file.name),
                    describeFileName(normalizedFile.name),
                )
        }

        fileCache.put(fileId, normalizedFile)

        return try {
            fileRepository.putFile(fileId, normalizedFile)
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

    private fun normalizeFileName(file: FilePayload): FilePayload {
        val normalizedName =
            file.name
                .trim()
                .replace(WHITESPACE_BEFORE_EXTENSION_REGEX, "$1")
                .let { Normalizer.normalize(it, Normalizer.Form.NFC) }

        return if (normalizedName == file.name) {
            file
        } else {
            file.copy(name = normalizedName)
        }
    }

    companion object {
        private val WHITESPACE_BEFORE_EXTENSION_REGEX = Regex("""\s+(\.[^.\s]+(?:\.[^.\s]+)*)$""")
    }

    private fun describeFileName(fileName: String): String {
        val visibleFileName =
            buildString {
                fileName.forEach { character ->
                    append(
                        when (character) {
                            '\n' -> "\\n"
                            '\r' -> "\\r"
                            '\t' -> "\\t"
                            else -> character
                        },
                    )
                }
            }
        val codePoints = fileName.codePoints().toArray().joinToString(" ") { "U+%04X".format(it) }

        return "\"$visibleFileName\" (length=${fileName.length}, codePoints=[$codePoints])"
    }
}
