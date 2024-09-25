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
    private final FileRepository fileRepository;

    @GetMapping("{fileId}")
    public Mono<ResponseEntity<File>> getFile(
            @PathVariable UUID fileId
    ) {
        return fileService.findById(fileId)
                .map(optionalFile -> optionalFile
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build())
                )
                .onErrorResume(
                        throwable -> Mono.just(ResponseEntity.internalServerError().build())
                );
    }

//    @PostMapping
//    public Mono<ResponseEntity<UUID>> addFile(
//            @RequestBody @Valid Mono<File> file
//    ) {
//        return Mono.zip(
//                        Mono.just(UUID.randomUUID()),
//                        file
//                )
//                .doOnNext(tuple -> fileService.putFile(tuple.getT1(), tuple.getT2()))
//                .flatMap(tuple -> fileRepository.putFile(tuple.getT1(), tuple.getT2()))
//                .doOnError(e -> log.error(String.valueOf(e)))
//                .map(id -> ResponseEntity.status(HttpStatus.CREATED).body(id));
//    }

    @PostMapping
    public Mono<ResponseEntity<UUID>> addFile(
            @RequestBody @Valid Mono<File> file
    ) {
        return file.flatMap(f -> {
            UUID fileId = UUID.randomUUID();
            return fileService.putFile(fileId, f)
                    .map(id -> ResponseEntity.status(HttpStatus.CREATED).body(id));
        }).doOnError(e -> log.error(String.valueOf(e)));
    }

}
