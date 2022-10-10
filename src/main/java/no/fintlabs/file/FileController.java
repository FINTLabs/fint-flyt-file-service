package no.fintlabs.file;

import no.fintlabs.cache.FintCache;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.UUID;

import static no.fintlabs.resourceserver.UrlPaths.EXTERNAL_API;

@RestController
@RequestMapping(EXTERNAL_API + "/filer")
public class FileController {

    private final FintCache<UUID, File> fileCache;
    private final FileRepository fileRepository;

    public FileController(FintCache<UUID, File> fileCache, FileRepository fileRepository) {
        this.fileCache = fileCache;
        this.fileRepository = fileRepository;
    }

    @GetMapping("{fileId}")
    public Mono<ResponseEntity<File>> file(
            @PathVariable UUID fileId
    ) {

        return
                fileCache.getOptional(fileId)
                        .map(Mono::just)
                        .orElseGet(() -> fileRepository.getFile(fileId))
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
                .flatMap(tuple -> fileRepository.putFile(tuple.getT1(), tuple.getT2()).then(Mono.just(tuple)))
                .map(tuple -> ResponseEntity.status(HttpStatus.CREATED).body(tuple.getT1()));
    }

}
