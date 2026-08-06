package no.novari.flyt.files.infrastructure.storage.azure

import com.azure.storage.blob.BlobServiceClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.test.util.ReflectionTestUtils
import java.util.UUID

@EnabledIfEnvironmentVariable(named = "AZURITE_CONNECTION_STRING", matches = ".+")
class AzureBlobAdapterIntegrationTest {
    @Test
    fun `openDownload returns null for missing blob`() {
        val connectionString = requireNotNull(System.getenv("AZURITE_CONNECTION_STRING"))
        val containerName = "files-${UUID.randomUUID()}"
        val containerClient =
            BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient()
                .getBlobContainerClient(containerName)
        containerClient.create()

        try {
            val adapter = AzureBlobAdapter(connectionString, containerName)
            ReflectionTestUtils.setField(adapter, "blobContainerClient", containerClient)

            val result = adapter.openDownload(UUID.randomUUID())

            assertThat(result).isNull()
        } finally {
            containerClient.delete()
        }
    }
}
