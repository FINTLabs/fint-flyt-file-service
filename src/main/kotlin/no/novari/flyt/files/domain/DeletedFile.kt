package no.novari.flyt.files.domain

import java.time.OffsetDateTime

data class DeletedFile(
    val name: String,
    val deletedAt: OffsetDateTime,
)
