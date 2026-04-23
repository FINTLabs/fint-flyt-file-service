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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilePayload) return false

        return name == other.name &&
            sourceApplicationId == other.sourceApplicationId &&
            sourceApplicationInstanceId == other.sourceApplicationInstanceId &&
            type == other.type &&
            encoding == other.encoding &&
            contents.contentEquals(other.contents)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (sourceApplicationId?.hashCode() ?: 0)
        result = 31 * result + (sourceApplicationInstanceId?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (encoding?.hashCode() ?: 0)
        result = 31 * result + contents.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "FilePayload(name=$name, sourceApplicationId=$sourceApplicationId, " +
            "sourceApplicationInstanceId=$sourceApplicationInstanceId, type=$type, " +
            "encoding=$encoding, contentsLength=${contents.size})"
    }
}
