package no.novari.flyt.files.domain

import java.io.InputStream

class FileDownload(
    val metadata: FileMetadata,
    val contents: InputStream,
)
