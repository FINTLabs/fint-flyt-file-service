package no.novari.flyt.files.domain.exception

import java.util.UUID

class FileNotFoundException(
    fileId: UUID,
) : RuntimeException("File with id $fileId was not found")
