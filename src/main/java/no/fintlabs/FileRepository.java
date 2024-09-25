package no.fintlabs;

import no.fintlabs.model.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
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

    private String generateDeleteBlobText(Long sourceApplicationId, String sourceApplicationInstanceId, String event) {
        return event + " with tags" +
                " sourceApplicationId=" + sourceApplicationId +
                " sourceApplicationInstanceId=" + sourceApplicationInstanceId;
    }

    private void logSuccessfulAction(UUID fileId, String performedAction) {
        log.info("Successfully {} File{fileId={}} in Azure Blob Storage", performedAction, fileId);
    }

    private void logUnsuccessfulAction(UUID fileId, String actionToBePerformed) {
        log.warn("Could not {} File{fileId={}} in Azure Blob Storage", actionToBePerformed, fileId);
    }

}
