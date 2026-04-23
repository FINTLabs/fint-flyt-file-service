package no.novari.flyt.files.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

class FilePayloadTest {
    @Test
    fun `toString redacts byte array contents`() {
        val payload =
            FilePayload(
                name = "example.pdf",
                sourceApplicationId = 123L,
                sourceApplicationInstanceId = "instance-1",
                type = MediaType.APPLICATION_PDF,
                encoding = "base64",
                contents = byteArrayOf(91, 32, 51, 32, 48),
            )

        val result = payload.toString()

        assertThat(result).contains("contentsLength=5")
        assertThat(result).doesNotContain("91, 32, 51, 32, 48")
        assertThat(result).doesNotContain("contents=[")
    }
}
