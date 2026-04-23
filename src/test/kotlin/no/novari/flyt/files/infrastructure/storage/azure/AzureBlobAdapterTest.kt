package no.novari.flyt.files.infrastructure.storage.azure

import org.apache.commons.text.StringEscapeUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AzureBlobAdapterTest {
    @Test
    fun `metadata encoding preserves decomposed unicode filename`() {
        val filename = "lønns arb vilkår st olav rog fylkl.pdf"

        val encoded = encodeMetadataValue(filename)

        assertThat(encoded).matches("""b64:[A-Za-z0-9_-]+""")
        assertThat(decodeMetadataValue(encoded)).isEqualTo(filename)
    }

    @Test
    fun `metadata decoding supports legacy html escaped filenames`() {
        val filename = "lønns arb vilkår st olav rog fylkl.pdf"
        val legacyEncoded = StringEscapeUtils.escapeHtml4(filename)

        assertThat(decodeMetadataValue(legacyEncoded)).isEqualTo(filename)
    }
}
