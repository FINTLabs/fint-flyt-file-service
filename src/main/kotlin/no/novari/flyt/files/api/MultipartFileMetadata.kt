package no.novari.flyt.files.api

import jakarta.validation.constraints.NotBlank
import no.novari.flyt.files.domain.FilePayload
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

data class MultipartFileMetadata(
    @field:NotBlank
    val name: String,
    val sourceApplicationId: Long,
    @field:NotBlank
    val sourceApplicationInstanceId: String,
    val type: String? = null,
    val encoding: String? = "binary",
) {
    fun toFilePayload(file: MultipartFile): FilePayload =
        FilePayload(
            name = name,
            sourceApplicationId = sourceApplicationId,
            sourceApplicationInstanceId = sourceApplicationInstanceId,
            type = resolveMediaType(file),
            encoding = encoding,
            contents = file.bytes,
        )

    private fun resolveMediaType(file: MultipartFile): MediaType =
        type
            ?.takeIf { it.isNotBlank() }
            ?.let(MediaType::parseMediaType)
            ?: file.contentType
                ?.takeIf { it.isNotBlank() }
                ?.let(MediaType::parseMediaType)
            ?: MediaType.APPLICATION_OCTET_STREAM
}
