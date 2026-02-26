package no.novari.flyt.files.api

import jakarta.validation.Valid
import no.novari.flyt.files.application.FileService
import no.novari.flyt.files.domain.FilePayload
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_CLIENT_API
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("$INTERNAL_CLIENT_API/filer")
class FileController(
    private val fileService: FileService,
) {
    @GetMapping("{fileId}")
    fun get(
        @PathVariable fileId: UUID,
    ): FilePayload {
        return fileService.findById(fileId)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun post(
        @RequestBody @Valid file: FilePayload,
    ): UUID {
        val fileId = UUID.randomUUID()
        return fileService.put(fileId, file)
    }
}
