package no.novari.flyt.files.domain

import org.springframework.http.MediaType
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer

data class FileMetadata(
    val name: String,
    val sourceApplicationId: Long? = null,
    val sourceApplicationInstanceId: String? = null,
    @get:JsonSerialize(using = ToStringSerializer::class)
    val type: MediaType? = null,
    val encoding: String? = null,
)
