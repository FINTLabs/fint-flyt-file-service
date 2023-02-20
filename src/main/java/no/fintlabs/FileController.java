package no.fintlabs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.cache.FintCache;
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

    private final FintCache<UUID, File> fileCache;
    private final FileRepository fileRepository;

    @GetMapping("{fileId}")
    public Mono<ResponseEntity<File>> getFile(
            @PathVariable UUID fileId
    ) {
        return
                fileCache.getOptional(fileId)
                        .map(Mono::just)
                        .orElseGet(() -> fileRepository.findById(fileId))
                        .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<UUID>> addFile(
            @RequestBody @Valid Mono<File> file
    ) {
        return Mono.zip(
                        Mono.just(UUID.randomUUID()),
                        file
                )
                .doOnNext(tuple -> fileCache.put(tuple.getT1(), tuple.getT2()))
                .flatMap(tuple -> fileRepository.putFile(tuple.getT1(), tuple.getT2()))
                .doOnError(e -> log.error(String.valueOf(e)))
                .map(id -> ResponseEntity.status(HttpStatus.CREATED).body(id));
    }

}
