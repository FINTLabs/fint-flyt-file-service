package no.fintlabs;

import no.fintlabs.model.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

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
                .doOnNext(response -> logSuccessfulAction(fileId, StringEscapeUtils.escapeHtml4(file.getName()), "uploaded"));
    }

    public Mono<File> findById(UUID fileId) {
        return azureBlobAdapter.downloadFile(fileId)
                .doOnNext(file -> logSuccessfulAction(fileId, StringEscapeUtils.unescapeHtml4(file.getName()), "downloaded"))
                .doOnError(e -> log.error("Could not download file", e));
    }

    public Mono<Void> deleteByTags(Long sourceApplicationId, String sourceApplicationInstanceId) {
        return azureBlobAdapter.deleteFilesByTags(sourceApplicationId, sourceApplicationInstanceId)
                .doOnError(e -> log.error(
                        generateDeleteBlobText(sourceApplicationId, sourceApplicationInstanceId, "Could not delete file"),
                        e
                ))
                .doOnSuccess(v -> log.info(generateDeleteBlobText(sourceApplicationId, sourceApplicationInstanceId, "Deleted files")));
    }

    private String generateDeleteBlobText(Long sourceApplicationId, String sourceApplicationInstanceId, String event) {
        return event + " with tags" +
                " sourceApplicationId=" + sourceApplicationId +
                " sourceApplicationInstanceId=" + sourceApplicationInstanceId;
    }

    private void logSuccessfulAction(UUID fileId, String fileName, String performedAction) {
        log.info("Successfully " + performedAction + " File{fileId=" + fileId + ", name=" + fileName + "} in Azure Blob Storage");
    }
}
