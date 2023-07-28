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
import no.fintlabs.model.File;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

@Service
public class AzureBlobAdapter {

    @Value("${fint.azure.storage-account.connection-string}")
    private String connectionString;

    @Value("${fint.azure.storage.container-blob.name}")
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
    }

    public Mono<UUID> uploadFile(UUID fileId, File file) {
        BlobAsyncClient blobAsyncClient = blobContainerAsyncClient.getBlobAsyncClient(fileId.toString());

        Flux<ByteBuffer> data = Flux.just(ByteBuffer.wrap(file.getContents()));

        Map<String, String> metadata = ImmutableMap.<String, String>builder()
                .put("name", StringEscapeUtils.escapeHtml4(file.getName()))
                .put("type", String.valueOf(file.getType()))
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
                .map(response -> fileId);
    }

    public Mono<File> downloadFile(UUID fileId) {
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
                .map(this::mapToFile);
    }

    public Mono<Void> deleteFilesByTags(Long sourceApplicationId, String sourceApplicationInstanceId) {
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
                .then();
    }

    private File mapToFile(BlobDownloadContentAsyncResponse blobDownloadContentAsyncResponse) {
        Map<String, String> metadata = blobDownloadContentAsyncResponse.getDeserializedHeaders().getMetadata();
        BinaryData value = blobDownloadContentAsyncResponse.getValue();
        return File.builder()
                .name(StringEscapeUtils.unescapeHtml4(metadata.get("name")))
                .type(MediaType.valueOf(metadata.get("type")))
                .encoding(metadata.get("encoding"))
                .contents(value.toBytes())
                .build();
    }
}
