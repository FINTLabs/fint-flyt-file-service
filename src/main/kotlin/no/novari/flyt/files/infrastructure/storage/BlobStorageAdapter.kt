package no.novari.flyt.files.infrastructure.storage

import no.novari.flyt.files.domain.DeletedFile
import no.novari.flyt.files.domain.FilePayload
import java.util.UUID

interface BlobStorageAdapter {
    fun uploadFile(
        fileId: UUID,
        file: FilePayload,
    ): UUID

    fun downloadFile(fileId: UUID): FilePayload?

    fun deleteFilesByIds(fileIds: List<UUID>)

    fun deleteFilesOlderThanDays(days: Int): List<DeletedFile>
}
