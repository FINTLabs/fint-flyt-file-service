package no.fintlabs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.model.File;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.UUID;

import static no.fintlabs.resourceserver.UrlPaths.INTERNAL_CLIENT_API;

@RestController
@RequiredArgsConstructor
@RequestMapping(INTERNAL_CLIENT_API + "/filer")
@Slf4j
public class FileController {

    private final FileService fileService;

    @GetMapping("{fileId}")
    public Mono<ResponseEntity<File>> get(
            @PathVariable UUID fileId
    ) {
        return fileService.findById(fileId)
                .map(optionalFile -> optionalFile
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build())
                )
                .onErrorResume(
                        throwable -> {
                            log.error("Could not get file", throwable);
                            return Mono.just(ResponseEntity.internalServerError().build());
                        }
                );
    }

    @PostMapping
    public Mono<ResponseEntity<UUID>> post(
            @RequestBody @Valid Mono<File> file
    ) {
        return file.flatMap(f -> {
            UUID fileId = UUID.randomUUID();
            return fileService.put(fileId, f)
                    .map(id -> ResponseEntity.status(HttpStatus.CREATED).body(id));
        }).onErrorResume(
                throwable -> {
                    log.error("Could not create file", throwable);
                    return Mono.just(ResponseEntity.internalServerError().build());
                }
        );
    }

}