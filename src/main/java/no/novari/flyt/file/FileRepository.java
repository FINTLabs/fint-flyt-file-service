package no.novari.flyt.file;

import lombok.extern.slf4j.Slf4j;
import no.novari.flyt.AzureBlobAdapter;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Slf4j
public class FileRepository {

    private final AzureBlobAdapter azureBlobAdapter;

    public FileRepository(AzureBlobAdapter azureBlobAdapter) {
        this.azureBlobAdapter = azureBlobAdapter;
    }

    public Mono<UUID> putFile(UUID fileId, File file) {
        return azureBlobAdapter.uploadFile(fileId, file)
                .doOnNext(response -> logSuccessfulAction(fileId, "uploaded"));
    }

    public Mono<Optional<File>> findById(UUID fileId) {
        return azureBlobAdapter.downloadFile(fileId)
                .doOnNext(optionalFile -> {
                    if (optionalFile.isPresent()) {
                        logSuccessfulAction(fileId, "found");
                    } else {
                        logUnsuccessfulAction(fileId, "find");
                    }
                })
                .doOnError(e -> log.error("Could not download file", e));
    }

    public Mono<Void> deleteFiles(List<UUID> fileIds) {
        return azureBlobAdapter.deleteFilesByIds(fileIds)
                .doOnSuccess(aVoid -> fileIds.forEach(fileId -> logSuccessfulAction(fileId, "deleted")))
                .doOnError(e -> log.error("Could not delete files", e));
    }

    public int deleteFilesOlderThan(int days) {
        List<DeletedFile> deletedFiles = azureBlobAdapter.deleteFilesOlderThanDays(days);
        deletedFiles
                .forEach(deletedFile ->
                        log.info("deleted file with name {}, timestamp {}", deletedFile.name(), deletedFile.deletedAt())
                );
        return deletedFiles.size();
    }

    private void logSuccessfulAction(UUID fileId, String performedAction) {
        log.info("Successfully {} File{fileId={}} in Azure Blob Storage", performedAction, fileId);
    }

    private void logUnsuccessfulAction(UUID fileId, String actionToBePerformed) {
        log.warn("Could not {} File{fileId={}} in Azure Blob Storage", actionToBePerformed, fileId);
    }

}
