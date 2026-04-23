package no.novari.flyt.files.domain

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.http.MediaType

data class FilePayload(
    @field:NotBlank
    val name: String,
    @field:NotNull
    val sourceApplicationId: Long? = null,
    @field:NotBlank
    val sourceApplicationInstanceId: String? = null,
    @get:JsonSerialize(using = ToStringSerializer::class)
    val type: MediaType? = null,
    val encoding: String? = null,
    @field:NotEmpty
    val contents: ByteArray,
)
