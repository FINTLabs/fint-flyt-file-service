package no.novari.flyt.files.api

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
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
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.async.CallableProcessingInterceptor
import org.springframework.web.context.request.async.WebAsyncUtils
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

@RestController
@RequestMapping("$INTERNAL_CLIENT_API/filer")
class FileController(
    private val fileService: FileService,
    private val objectMapper: ObjectMapper,
) {
    @GetMapping("{fileId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(
        @PathVariable fileId: UUID,
        webRequest: NativeWebRequest,
    ): ResponseEntity<StreamingResponseBody> {
        val fileDownload = fileService.openDownload(fileId)
        val contentsClosed = AtomicBoolean(false)
        val closeContents = {
            if (contentsClosed.compareAndSet(false, true)) {
                fileDownload.contents.close()
            }
        }
        registerAsyncCleanup(webRequest, fileId, closeContents)

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                StreamingResponseBody { outputStream ->
                    try {
                        objectMapper.factory.createGenerator(outputStream).use { generator ->
                            writeFileDownload(generator, fileDownload)
                        }
                    } finally {
                        closeContents()
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
        val metadataFields = objectMapper.valueToTree<ObjectNode>(metadata).properties()
        for (field in metadataFields) {
            generator.writeFieldName(field.key)
            generator.writeTree(field.value)
        }
        generator.writeFieldName("contents")
        generator.writeBinary(fileDownload.contents, -1)
        generator.writeEndObject()
    }

    private fun registerAsyncCleanup(
        webRequest: NativeWebRequest,
        fileId: UUID,
        closeContents: () -> Unit,
    ) {
        WebAsyncUtils
            .getAsyncManager(webRequest)
            .registerCallableInterceptor(
                "fileDownload-$fileId",
                object : CallableProcessingInterceptor {
                    override fun <T> handleTimeout(
                        request: NativeWebRequest,
                        task: Callable<T>,
                    ): Any {
                        closeContents()
                        return CallableProcessingInterceptor.RESULT_NONE
                    }

                    override fun <T> handleError(
                        request: NativeWebRequest,
                        task: Callable<T>,
                        throwable: Throwable,
                    ): Any {
                        closeContents()
                        return CallableProcessingInterceptor.RESULT_NONE
                    }

                    override fun <T> afterCompletion(
                        request: NativeWebRequest,
                        task: Callable<T>,
                    ) {
                        closeContents()
                    }
                },
            )
    }
}
