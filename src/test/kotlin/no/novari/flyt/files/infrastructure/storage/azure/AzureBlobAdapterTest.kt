package no.novari.flyt.files.infrastructure.storage.azure

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobProperties
import com.azure.storage.blob.models.BlobStorageException
import com.azure.storage.blob.options.BlobInputStreamOptions
import com.azure.storage.blob.specialized.BlobInputStream
import org.apache.commons.text.StringEscapeUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import java.util.UUID

class AzureBlobAdapterTest {
    @Test
    fun `openDownload opens stream and maps metadata from stream properties`() {
        val fileId = UUID.fromString("347b5218-6f55-4bea-917a-581e81fe0326")
        val blobContainerClient = mock<BlobContainerClient>()
        val blobClient = mock<BlobClient>()
        val contents = mock<BlobInputStream>()
        val properties = mock<BlobProperties>()
        val adapter = AzureBlobAdapter("connection-string", "container")
        ReflectionTestUtils.setField(adapter, "blobContainerClient", blobContainerClient)

        whenever(blobContainerClient.getBlobClient(fileId.toString())).thenReturn(blobClient)
        whenever(blobClient.openInputStream(any<BlobInputStreamOptions>())).thenReturn(contents)
        whenever(contents.properties).thenReturn(properties)
        whenever(properties.metadata).thenReturn(
            mapOf(
                "name" to encodeMetadataValue("document.pdf"),
                "sourceApplicationId" to "123",
                "sourceApplicationInstanceId" to "instance-1",
                "type" to MediaType.APPLICATION_PDF_VALUE,
                "encoding" to "binary",
            ),
        )

        val result = adapter.openDownload(fileId)

        assertThat(result?.metadata?.name).isEqualTo("document.pdf")
        assertThat(result?.metadata?.sourceApplicationId).isEqualTo(123L)
        assertThat(result?.metadata?.sourceApplicationInstanceId).isEqualTo("instance-1")
        assertThat(result?.metadata?.type).isEqualTo(MediaType.APPLICATION_PDF)
        assertThat(result?.metadata?.encoding).isEqualTo("binary")
        assertThat(result?.contents).isSameAs(contents)
        assertInputStreamOptions(blobClient)
    }

    @Test
    fun `openDownload returns null when blob does not exist`() {
        val fileId = UUID.fromString("938589cc-7b0f-4be2-8c3e-8790d68ce1da")
        val blobContainerClient = mock<BlobContainerClient>()
        val blobClient = mock<BlobClient>()
        val notFound = mock<BlobStorageException>()
        val adapter = AzureBlobAdapter("connection-string", "container")
        ReflectionTestUtils.setField(adapter, "blobContainerClient", blobContainerClient)

        whenever(blobContainerClient.getBlobClient(fileId.toString())).thenReturn(blobClient)
        whenever(notFound.statusCode).thenReturn(HttpStatus.NOT_FOUND.value())
        whenever(blobClient.openInputStream(any<BlobInputStreamOptions>())).thenThrow(notFound)

        val result = adapter.openDownload(fileId)

        assertThat(result).isNull()
        assertInputStreamOptions(blobClient)
    }

    @Test
    fun `openDownload closes stream when metadata mapping fails`() {
        val fileId = UUID.fromString("05bd0291-6c35-4888-984e-06ee656ff9dd")
        val blobContainerClient = mock<BlobContainerClient>()
        val blobClient = mock<BlobClient>()
        val contents = mock<BlobInputStream>()
        val properties = mock<BlobProperties>()
        val adapter = AzureBlobAdapter("connection-string", "container")
        ReflectionTestUtils.setField(adapter, "blobContainerClient", blobContainerClient)

        whenever(blobContainerClient.getBlobClient(fileId.toString())).thenReturn(blobClient)
        whenever(blobClient.openInputStream(any<BlobInputStreamOptions>())).thenReturn(contents)
        whenever(contents.properties).thenReturn(properties)
        whenever(properties.metadata).thenReturn(
            mapOf(
                "name" to encodeMetadataValue("document.pdf"),
                "type" to "not a media type",
            ),
        )

        assertThrows<InvalidMediaTypeException> {
            adapter.openDownload(fileId)
        }

        verify(contents).close()
        assertInputStreamOptions(blobClient)
    }

    @Test
    fun `metadata encoding preserves decomposed unicode filename`() {
        val filename = "lønns arb vilkår rog fylkl.pdf"

        val encoded = encodeMetadataValue(filename)

        assertThat(encoded).matches("""b64:[A-Za-z0-9_-]+""")
        assertThat(decodeMetadataValue(encoded)).isEqualTo(filename)
    }

    @Test
    fun `metadata decoding supports legacy html escaped filenames`() {
        val filename = "lønns arb vilkår rog fylkl.pdf"
        val legacyEncoded = StringEscapeUtils.escapeHtml4(filename)

        assertThat(decodeMetadataValue(legacyEncoded)).isEqualTo(filename)
    }

    @Test
    fun `metadata encoding preserves en dash in filename`() {
        val filename = "Example document – applicant copy.pdf"

        val encoded = encodeMetadataValue(filename)

        assertThat(decodeMetadataValue(encoded)).isEqualTo(filename)
    }

    private fun assertInputStreamOptions(blobClient: BlobClient) {
        val optionsCaptor = argumentCaptor<BlobInputStreamOptions>()

        verify(blobClient).openInputStream(optionsCaptor.capture())

        assertThat(optionsCaptor.firstValue.blockSize).isEqualTo(2 * 1024 * 1024)
    }
}
