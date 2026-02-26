package no.novari.flyt.files.domain.exception

class FileStorageException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
