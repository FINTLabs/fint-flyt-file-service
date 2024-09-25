package no.fintlabs;

import lombok.AllArgsConstructor;
import no.fintlabs.cache.FintCache;
import no.fintlabs.flyt.kafka.headers.InstanceFlowHeaders;
import no.fintlabs.model.File;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
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

    public Mono<UUID> putFile(UUID fileId, File file) {
        try {
            fileCache.put(fileId, file);
            return fileRepository.putFile(fileId, file)
                    .thenReturn(fileId);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public void cleanupFiles(InstanceFlowHeaders instanceFlowHeaders) {
        deleteFileFromCache(instanceFlowHeaders.getFileIds());
        deleteFileFromBlobStorage(instanceFlowHeaders.getFileIds());
    }

    private void deleteFileFromCache(List<UUID> fileIds) {

    }

    private void deleteFileFromBlobStorage(List<UUID> fileIds) {
        fileRepository.deleteFiles(fileIds);
    }
}
