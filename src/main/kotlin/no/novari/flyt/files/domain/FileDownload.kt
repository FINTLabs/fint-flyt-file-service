package no.novari.flyt.files.domain

import java.io.InputStream

data class FileDownload(
    val metadata: FileMetadata,
    val openContents: () -> InputStream,
)
