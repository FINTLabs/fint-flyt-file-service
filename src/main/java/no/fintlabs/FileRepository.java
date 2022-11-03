package no.fintlabs;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.options.FindBlobsOptions;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.model.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

@Repository
@Slf4j
public class FileRepository {

    @Value("${fint.flyt.internal-files.connection-string}")
    private String connectionString;

    @Value("${fint.flyt.internal-files.container-name}")
    private String containerName;

    private BlobContainerAsyncClient blobContainerAsyncClient;

    private final long blockSize = 2L * 1024L * 1024L;

    @PostConstruct
    public void init() {
        BlobServiceAsyncClient blobServiceAsyncClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildAsyncClient();
        blobContainerAsyncClient = blobServiceAsyncClient
                .getBlobContainerAsyncClient(containerName);
        blobContainerAsyncClient.exists()
                .filter(b -> !b)
                .map(b -> blobContainerAsyncClient.create())
                .block();
        log.info("Connected to {}", blobServiceAsyncClient.getAccountName());
    }

    public Mono<UUID> putFile(UUID fileId, File file) {

        BlobAsyncClient blobAsyncClient = blobContainerAsyncClient.getBlobAsyncClient(fileId.toString());

        Flux<ByteBuffer> data = Flux.just(ByteBuffer.wrap(file.getContents()));

        Map<String, String> metadata = ImmutableMap.<String, String>builder()
                .put("name", file.getName())
                .put("type", file.getType())
                .put("encoding", file.getEncoding())
                .build();

        Map<String, String> tags = ImmutableMap.<String, String>builder()
                .put("sourceApplicationId", String.valueOf(file.getSourceApplicationId()))
                .put("sourceApplicationInstanceId", file.getSourceApplicationInstanceId())
                .build();

        return blobAsyncClient
                .uploadWithResponse(
                        new BlobParallelUploadOptions(data)
                                .setParallelTransferOptions(new ParallelTransferOptions().setBlockSizeLong(blockSize))
                                .setHeaders(new BlobHttpHeaders())
                                .setMetadata(metadata)
                                .setTags(tags)
                                .setTier(AccessTier.HOT)
                )
                .map(response -> fileId)
                .doOnNext(response -> logSuccessfulAction(fileId, file.getName(), "uploaded"));
    }

    public Mono<File> findById(UUID fileId) {
        BlobAsyncClient blobAsyncClient = blobContainerAsyncClient.getBlobAsyncClient(fileId.toString());
        DownloadRetryOptions options = new DownloadRetryOptions().setMaxRetryRequests(3);

        return blobAsyncClient
                .downloadContentWithResponse(options, new BlobRequestConditions())
                .<BlobDownloadContentAsyncResponse>handle((response, sink) -> {
                    if (response.getStatusCode() == 200) {
                        sink.next(response);
                    } else {
                        sink.error(new RuntimeException("Received response that was not 200 OK: " + response));
                    }
                })
                .map(this::mapToFile)
                .doOnNext(file -> logSuccessfulAction(fileId, file.getName(), "downloaded"))
                .doOnError(e -> log.error("Could not download file", e));
    }

    public Mono<Void> deleteByTags(Long sourceApplicationId, String sourceApplicationInstanceId) {
        return blobContainerAsyncClient
                .findBlobsByTags(new FindBlobsOptions(String.format(
                        "\"sourceApplicationId\"='%s'And\"sourceApplicationInstanceId\"='%s'",
                        sourceApplicationId, sourceApplicationInstanceId
                )))
                .map(TaggedBlobItem::getName)
                .map(blobContainerAsyncClient::getBlobAsyncClient)
                .flatMap(blobAsyncClient -> blobAsyncClient
                        .deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null)
                )
                .doOnError(e -> log.error(

                        generateDeleteBlobText(sourceApplicationId, sourceApplicationInstanceId, "Could not delete file"),
                        e
                ))
                .then()
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

    private File mapToFile(BlobDownloadContentAsyncResponse blobDownloadContentAsyncResponse) {
        Map<String, String> metadata = blobDownloadContentAsyncResponse.getDeserializedHeaders().getMetadata();
        BinaryData value = blobDownloadContentAsyncResponse.getValue();
        return File.builder()
                .name(metadata.get("name"))
                .type(metadata.get("type"))
                .encoding(metadata.get("encoding"))
                .contents(value.toBytes())
                .build();
    }

}
