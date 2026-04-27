package no.novari.flyt.files.infrastructure.storage.azure

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.AccessTier
import com.azure.storage.blob.models.BlobDownloadContentResponse
import com.azure.storage.blob.models.BlobHttpHeaders
import com.azure.storage.blob.models.BlobListDetails
import com.azure.storage.blob.models.BlobRequestConditions
import com.azure.storage.blob.models.BlobStorageException
import com.azure.storage.blob.models.DeleteSnapshotsOptionType
import com.azure.storage.blob.models.DownloadRetryOptions
import com.azure.storage.blob.models.ListBlobsOptions
import com.azure.storage.blob.models.ParallelTransferOptions
import com.azure.storage.blob.options.BlobParallelUploadOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import no.novari.flyt.files.domain.DeletedFile
import no.novari.flyt.files.domain.FilePayload
import no.novari.flyt.files.infrastructure.storage.BlobStorageAdapter
import org.apache.commons.text.StringEscapeUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

@Service
@Profile("!local-staging")
class AzureBlobAdapter(
    @param:Value("\${fint.azure.storage-account.connection-string}")
    private val connectionString: String,
    @param:Value("\${fint.azure.storage.container-blob.name}")
    private val containerName: String,
) : BlobStorageAdapter {
    private val log = KotlinLogging.logger {}
    private lateinit var blobContainerClient: BlobContainerClient

    @PostConstruct
    fun init() {
        val blobServiceClient =
            BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient()
        blobContainerClient = blobServiceClient.getBlobContainerClient(containerName)

        if (!blobContainerClient.exists()) {
            blobContainerClient.create()
        }
    }

    override fun uploadFile(
        fileId: UUID,
        file: FilePayload,
    ): UUID {
        val blobClient = blobContainerClient.getBlobClient(fileId.toString())
        val data = BinaryData.fromBytes(file.contents)
        val encodedFileName = encodeMetadataValue(file.name)

        log.atDebug {
            message = "Encoding file metadata for fileId={} fileName={} encodedMetadataName={}"
            arguments =
                arrayOf(
                    fileId,
                    describeFileName(file.name),
                    encodedFileName,
                )
        }

        val metadata =
            buildMap {
                put(METADATA_NAME, encodedFileName)
                file.type?.let { put(METADATA_TYPE, it.toString()) }
                file.encoding?.let { put(METADATA_ENCODING, it) }
                file.sourceApplicationId?.let { put(METADATA_SOURCE_APPLICATION_ID, it.toString()) }
                file.sourceApplicationInstanceId?.let { put(METADATA_SOURCE_APPLICATION_INSTANCE_ID, it) }
            }

        val tags =
            buildMap {
                file.sourceApplicationId?.let { put(METADATA_SOURCE_APPLICATION_ID, it.toString()) }
                file.sourceApplicationInstanceId?.let { put(METADATA_SOURCE_APPLICATION_INSTANCE_ID, it) }
            }

        blobClient
            .uploadWithResponse(
                BlobParallelUploadOptions(data)
                    .setParallelTransferOptions(ParallelTransferOptions().setBlockSizeLong(BLOCK_SIZE))
                    .setHeaders(BlobHttpHeaders())
                    .setMetadata(metadata)
                    .setTags(tags)
                    .setTier(AccessTier.HOT),
                null,
                null,
            )

        return fileId
    }

    override fun downloadFile(fileId: UUID): FilePayload? {
        val blobClient = blobContainerClient.getBlobClient(fileId.toString())
        val options = DownloadRetryOptions().setMaxRetryRequests(3)

        return try {
            val response =
                blobClient.downloadContentWithResponse(
                    options,
                    BlobRequestConditions(),
                    null,
                    null,
                )

            when (response.statusCode) {
                HttpStatus.OK.value() -> mapToFile(fileId, response)
                HttpStatus.NOT_FOUND.value() -> null
                else -> throw RuntimeException("Received response that was not 200 OK: $response")
            }
        } catch (exception: BlobStorageException) {
            if (exception.statusCode == HttpStatus.NOT_FOUND.value()) {
                null
            } else {
                throw exception
            }
        }
    }

    override fun deleteFilesByIds(fileIds: List<UUID>) {
        fileIds
            .asSequence()
            .map(UUID::toString)
            .map(blobContainerClient::getBlobClient)
            .forEach { blobClient ->
                blobClient.deleteIfExistsWithResponse(
                    DeleteSnapshotsOptionType.INCLUDE,
                    null,
                    null,
                    null,
                )
            }
    }

    override fun deleteFilesOlderThanDays(days: Int): List<DeletedFile> {
        val cutoff = OffsetDateTime.now().minusDays(days.toLong())

        return blobContainerClient
            .listBlobs(
                ListBlobsOptions().setDetails(
                    BlobListDetails().setRetrieveMetadata(true),
                ),
                null,
            ).asSequence()
            .filter { blobItem ->
                val lastModified = blobItem.properties.lastModified
                lastModified != null && lastModified.isBefore(cutoff)
            }.map { blobItem ->
                val name = blobItem.name
                val lastModified =
                    requireNotNull(blobItem.properties.lastModified) {
                        "lastModified was null for blob=$name"
                    }
                val client = blobContainerClient.getBlobClient(name)

                client.deleteIfExistsWithResponse(
                    DeleteSnapshotsOptionType.INCLUDE,
                    null,
                    null,
                    null,
                )

                DeletedFile(name, lastModified)
            }.toList()
            .sortedBy(DeletedFile::deletedAt)
    }

    private fun mapToFile(
        fileId: UUID,
        blobDownloadContentResponse: BlobDownloadContentResponse,
    ): FilePayload {
        val metadata = blobDownloadContentResponse.deserializedHeaders.metadata.orEmpty()
        val value = blobDownloadContentResponse.value
        val encodedFileName = metadata[METADATA_NAME].orEmpty()
        val decodedFileName = decodeMetadataValue(encodedFileName)

        log.atDebug {
            message = "Decoded file metadata for fileId={} encodedMetadataName={} decodedFileName={}"
            arguments =
                arrayOf(
                    fileId,
                    encodedFileName,
                    describeFileName(decodedFileName),
                )
        }

        return FilePayload(
            name = decodedFileName,
            sourceApplicationId = metadata[METADATA_SOURCE_APPLICATION_ID]?.toLongOrNull(),
            sourceApplicationInstanceId = metadata[METADATA_SOURCE_APPLICATION_INSTANCE_ID],
            type = metadata[METADATA_TYPE]?.let(MediaType::valueOf),
            encoding = metadata[METADATA_ENCODING],
            contents = value.toBytes(),
        )
    }

    private fun describeFileName(fileName: String): String {
        val visibleFileName =
            buildString {
                fileName.forEach { character ->
                    append(
                        when (character) {
                            '\n' -> "\\n"
                            '\r' -> "\\r"
                            '\t' -> "\\t"
                            else -> character
                        },
                    )
                }
            }
        val codePoints = fileName.codePoints().toArray().joinToString(" ") { "U+%04X".format(it) }

        return "\"$visibleFileName\" (length=${fileName.length}, codePoints=[$codePoints])"
    }

    companion object {
        private const val BLOCK_SIZE = 2L * 1024L * 1024L
        private const val METADATA_NAME = "name"
        private const val METADATA_TYPE = "type"
        private const val METADATA_ENCODING = "encoding"
        private const val METADATA_SOURCE_APPLICATION_ID = "sourceApplicationId"
        private const val METADATA_SOURCE_APPLICATION_INSTANCE_ID = "sourceApplicationInstanceId"
    }
}

private const val METADATA_ENCODING_PREFIX = "b64:"

internal fun encodeMetadataValue(value: String): String {
    val encoded =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    return "$METADATA_ENCODING_PREFIX$encoded"
}

internal fun decodeMetadataValue(value: String): String {
    if (!value.startsWith(METADATA_ENCODING_PREFIX)) {
        return StringEscapeUtils.unescapeHtml4(value)
    }

    val encodedValue = value.removePrefix(METADATA_ENCODING_PREFIX)
    return String(Base64.getUrlDecoder().decode(encodedValue), StandardCharsets.UTF_8)
}
