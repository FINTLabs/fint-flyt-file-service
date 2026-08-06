package no.novari.flyt.files.domain

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.springframework.http.MediaType

data class FileMetadata(
    val name: String,
    val sourceApplicationId: Long? = null,
    val sourceApplicationInstanceId: String? = null,
    @get:JsonSerialize(using = ToStringSerializer::class)
    val type: MediaType? = null,
    val encoding: String? = null,
)
