package no.novari.flyt.files.infrastructure.storage.inmemory

import no.novari.flyt.files.domain.DeletedFile
import no.novari.flyt.files.domain.FilePayload
import no.novari.flyt.files.infrastructure.storage.BlobStorageAdapter
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
@Profile("local-staging")
class InMemoryBlobAdapter : BlobStorageAdapter {
    private val files = ConcurrentHashMap<UUID, StoredFile>()

    override fun uploadFile(
        fileId: UUID,
        file: FilePayload,
    ): UUID {
        files[fileId] = StoredFile(copyPayload(file), OffsetDateTime.now())
        return fileId
    }

    override fun downloadFile(fileId: UUID): FilePayload? {
        val storedFile = files[fileId]
        if (storedFile == null) {
            return null
        }

        return copyPayload(storedFile.payload)
    }

    override fun deleteFilesByIds(fileIds: List<UUID>) {
        fileIds.forEach { fileId ->
            files.remove(fileId)
        }
    }

    override fun deleteFilesOlderThanDays(days: Int): List<DeletedFile> {
        val cutoff = OffsetDateTime.now().minusDays(days.toLong())
        val deletedFiles = mutableListOf<DeletedFile>()

        files.entries
            .filter { entry ->
                entry.value.uploadedAt.isBefore(cutoff)
            }.forEach { entry ->
                if (files.remove(entry.key, entry.value)) {
                    deletedFiles.add(
                        DeletedFile(
                            name = entry.value.payload.name,
                            deletedAt = entry.value.uploadedAt,
                        ),
                    )
                }
            }

        return deletedFiles.sortedBy(DeletedFile::deletedAt)
    }

    private fun copyPayload(filePayload: FilePayload): FilePayload {
        return filePayload.copy(contents = filePayload.contents.copyOf())
    }

    private data class StoredFile(
        val payload: FilePayload,
        val uploadedAt: OffsetDateTime,
    )
}
