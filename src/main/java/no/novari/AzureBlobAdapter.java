package no.novari;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.AccessTier;
import com.azure.storage.blob.models.BlobDownloadContentAsyncResponse;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.PostConstruct;
import no.novari.file.DeletedFile;
import no.novari.file.File;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AzureBlobAdapter {

    @Value("${fint.azure.storage-account.connection-string}")
    private String connectionString;

    @Value("${fint.azure.storage.container-blob.name}")
    private String containerName;

    private BlobContainerAsyncClient blobContainerAsyncClient;

    private static final long BLOCK_SIZE = 2L * 1024L * 1024L;

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
                                .setParallelTransferOptions(new ParallelTransferOptions().setBlockSizeLong(BLOCK_SIZE))
                                .setHeaders(new BlobHttpHeaders())
                                .setMetadata(metadata)
                                .setTags(tags)
                                .setTier(AccessTier.HOT)
                )
                .map(response -> fileId);
    }

    public Mono<Optional<File>> downloadFile(UUID fileId) {
        BlobAsyncClient blobAsyncClient = blobContainerAsyncClient.getBlobAsyncClient(fileId.toString());
        DownloadRetryOptions options = new DownloadRetryOptions().setMaxRetryRequests(3);

        return blobAsyncClient
                .downloadContentWithResponse(options, new BlobRequestConditions())
                .handle((response, sink) -> {
                    if (response.getStatusCode() == 200) {
                        sink.next(Optional.of(mapToFile(response)));
                    } else if (response.getStatusCode() == 404) {
                        sink.next(Optional.empty());
                    } else {
                        sink.error(new RuntimeException("Received response that was not 200 OK: " + response));
                    }
                });
    }

    public Mono<Void> deleteFilesByIds(List<UUID> fileIds) {
        return Flux.fromIterable(fileIds)
                .map(UUID::toString)
                .map(blobContainerAsyncClient::getBlobAsyncClient)
                .flatMap(blobAsyncClient -> blobAsyncClient.deleteIfExistsWithResponse(
                        DeleteSnapshotsOptionType.INCLUDE, null)
                )
                .then();
    }

    public List<DeletedFile> deleteFilesOlderThanDays(int days) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(days);

        return blobContainerAsyncClient
                .listBlobs(
                        new ListBlobsOptions().setDetails(
                                new BlobListDetails().setRetrieveMetadata(true)
                        ),
                        null
                )
                .filter(blobItem -> {
                    OffsetDateTime lastMod = blobItem.getProperties().getLastModified();
                    return lastMod != null && lastMod.isBefore(cutoff);
                })
                .flatMap(blobItem -> {
                    String name = blobItem.getName();
                    OffsetDateTime lastMod = blobItem.getProperties().getLastModified();
                    BlobAsyncClient client = blobContainerAsyncClient.getBlobAsyncClient(name);

                    return client
                            .deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null)
                            .map(response -> new DeletedFile(name, lastMod));
                })
                .sort(Comparator.comparing(DeletedFile::deletedAt))
                .collectList()
                .block();
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
