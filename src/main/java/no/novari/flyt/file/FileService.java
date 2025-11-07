package no.novari.flyt.file;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.cache.FintCache;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class FileService {

    private final FintCache<UUID, File> fileCache;
    private final FileRepository fileRepository;

    public Mono<Optional<File>> findById(UUID fileId) {
        try {
            Optional<File> optionalFileFromCache = fileCache.getOptional(fileId);
            if (optionalFileFromCache.isPresent()) {
                return Mono.just(optionalFileFromCache);
            }
            return fileRepository.findById(fileId);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public Mono<UUID> put(UUID fileId, File file) {
        try {
            if (Objects.isNull(fileId)) {
                return Mono.error(new IllegalArgumentException("FileId cannot be null"));
            }
            if (Objects.isNull(file)) {
                return Mono.error(new IllegalArgumentException("File cannot be null"));
            }
            fileCache.put(fileId, file);
        } catch (Exception e) {
            return Mono.error(e);
        }
        try {
            return fileRepository.putFile(fileId, file)
                    .doOnError(e -> fileCache.remove(fileId))
                    .thenReturn(fileId);
        } catch (Exception e) {
            fileCache.remove(fileId);
            return Mono.error(e);
        }
    }

    public Mono<Void> delete(List<UUID> fileIds) {
        try {
            if (Objects.isNull(fileIds) || fileIds.isEmpty()) {
                log.info("List of fileIds is empty");
                return Mono.empty();
            }
            fileCache.remove(fileIds);
            return fileRepository.deleteFiles(fileIds);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public int deleteFilesOlderThan(int days) {
        return fileRepository.deleteFilesOlderThan(days);
    }

}