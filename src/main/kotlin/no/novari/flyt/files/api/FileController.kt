package no.novari.flyt.files.api

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Valid
import no.novari.flyt.files.application.FileService
import no.novari.flyt.files.domain.FileDownload
import no.novari.flyt.files.domain.FilePayload
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_CLIENT_API
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.util.UUID

@RestController
@RequestMapping("$INTERNAL_CLIENT_API/filer")
class FileController(
    private val fileService: FileService,
    private val objectMapper: ObjectMapper,
) {
    @GetMapping("{fileId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(
        @PathVariable fileId: UUID,
    ): ResponseEntity<StreamingResponseBody> {
        val fileDownload = fileService.openDownload(fileId)

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                StreamingResponseBody { outputStream ->
                    objectMapper.factory.createGenerator(outputStream).use { generator ->
                        writeFileDownload(generator, fileDownload)
                    }
                },
            )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun post(
        @RequestBody @Valid file: FilePayload,
    ): UUID {
        val fileId = UUID.randomUUID()
        return fileService.put(fileId, file)
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun postMultipart(
        @RequestPart("metadata") @Valid metadata: MultipartFileMetadata,
        @RequestPart("file") file: MultipartFile,
    ): UUID {
        val fileId = UUID.randomUUID()
        return fileService.put(fileId, metadata.toFilePayload(file))
    }

    private fun writeFileDownload(
        generator: JsonGenerator,
        fileDownload: FileDownload,
    ) {
        val metadata = fileDownload.metadata

        generator.writeStartObject()
        generator.writeStringField("name", metadata.name)
        generator.writeFieldName("sourceApplicationId")
        metadata.sourceApplicationId?.let(generator::writeNumber) ?: generator.writeNull()
        generator.writeFieldName("sourceApplicationInstanceId")
        metadata.sourceApplicationInstanceId?.let(generator::writeString) ?: generator.writeNull()
        generator.writeFieldName("type")
        metadata.type?.toString()?.let(generator::writeString) ?: generator.writeNull()
        generator.writeFieldName("encoding")
        metadata.encoding?.let(generator::writeString) ?: generator.writeNull()
        generator.writeFieldName("contents")
        fileDownload.openContents().use { contents ->
            generator.writeBinary(contents, -1)
        }
        generator.writeEndObject()
    }
}
