package no.fintlabs.file;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.*;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
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

    private BlobServiceAsyncClient blobServiceAsyncClient;
    private BlobContainerAsyncClient blobContainerAsyncClient;

    private BlobRequestConditions requestConditions;
    private long blockSize = 2L * 1024L * 1024L;

    @PostConstruct
    public void init() {
        log.info("test" + connectionString.toString());
        blobServiceAsyncClient = new BlobServiceClientBuilder()
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
        ParallelTransferOptions parallelTransferOptions = new ParallelTransferOptions().setBlockSizeLong(blockSize);

        Map<String, String> metadata = ImmutableMap.<String, String>builder()
                .put("name", file.getName())
                .put("type", file.getType())
                .put("encoding", file.getEncoding())
                .build();

        BlobHttpHeaders blobHttpHeaders = new BlobHttpHeaders();

        return blobAsyncClient
                .uploadWithResponse(data, parallelTransferOptions, blobHttpHeaders, metadata, AccessTier.HOT, requestConditions)
                .map(response -> fileId)
                .doOnNext(response -> logSuccessfulAction(fileId, file.getName(), "uploaded"));
    }

    public Mono<File> getFile(UUID fileId) {
        BlobAsyncClient blobAsyncClient = blobContainerAsyncClient.getBlobAsyncClient(fileId.toString());
        DownloadRetryOptions options = new DownloadRetryOptions().setMaxRetryRequests(3);

        return blobAsyncClient
                .downloadContentWithResponse(options, requestConditions)
                .map(this::mapToFile)
                .doOnNext(file -> logSuccessfulAction(fileId, file.getName(), "downloaded"));
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
