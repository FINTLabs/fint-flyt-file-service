package no.novari.flyt.files.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.novari.flyt.files.application.FileService
import no.novari.flyt.files.domain.FileDownload
import no.novari.flyt.files.domain.FileMetadata
import no.novari.flyt.files.domain.FilePayload
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.io.ByteArrayInputStream
import java.util.UUID

class FileControllerTest {
    private val objectMapper = jacksonObjectMapper()
    private lateinit var fileService: FileService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        fileService = mock()
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(FileController(fileService, objectMapper))
                .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .build()
    }

    @Test
    fun `get streams JSON base64 file payload`() {
        val fileId = UUID.fromString("f4dc9501-9af0-4e5d-a8a4-064b3d615d52")
        val fileDownload =
            FileDownload(
                metadata =
                    FileMetadata(
                        name = "document.pdf",
                        sourceApplicationId = 123L,
                        sourceApplicationInstanceId = "instance-1",
                        type = MediaType.APPLICATION_PDF,
                        encoding = "binary",
                    ),
                openContents = {
                    ByteArrayInputStream(byteArrayOf(1, 2, 3))
                },
            )
        whenever(fileService.openDownload(fileId)).thenReturn(fileDownload)

        val mvcResult =
            mockMvc
                .perform(get("$PATH/$fileId"))
                .andExpect(request().asyncStarted())
                .andReturn()

        mockMvc
            .perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                      "name": "document.pdf",
                      "sourceApplicationId": 123,
                      "sourceApplicationInstanceId": "instance-1",
                      "type": "application/pdf",
                      "encoding": "binary",
                      "contents": "AQID"
                    }
                    """.trimIndent(),
                ),
            )
    }

    @Test
    fun `post stores JSON base64 upload unchanged`() {
        val fileId = UUID.fromString("60a2f6ed-d6d0-491c-9494-f7cf97fb43de")
        val payload =
            FilePayload(
                name = "document.pdf",
                sourceApplicationId = 123L,
                sourceApplicationInstanceId = "instance-1",
                type = MediaType.APPLICATION_PDF,
                encoding = "base64",
                contents = byteArrayOf(1, 2, 3),
            )
        whenever(fileService.put(any(), any())).thenReturn(fileId)

        mockMvc
            .perform(
                post(PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().string(objectMapper.writeValueAsString(fileId)))

        val fileCaptor = argumentCaptor<FilePayload>()
        verify(fileService).put(any(), fileCaptor.capture())
        assertThat(fileCaptor.firstValue.name).isEqualTo("document.pdf")
        assertThat(fileCaptor.firstValue.sourceApplicationId).isEqualTo(123L)
        assertThat(fileCaptor.firstValue.sourceApplicationInstanceId).isEqualTo("instance-1")
        assertThat(fileCaptor.firstValue.type).isEqualTo(MediaType.APPLICATION_PDF)
        assertThat(fileCaptor.firstValue.encoding).isEqualTo("base64")
        assertThat(fileCaptor.firstValue.contents).containsExactly(1, 2, 3)
    }

    @Test
    fun `postMultipart stores binary upload as file payload`() {
        val fileId = UUID.fromString("e1eb6bf8-21dd-4ac9-a9d4-e42f5863b56b")
        val metadata =
            MultipartFileMetadata(
                name = "document.pdf",
                sourceApplicationId = 123L,
                sourceApplicationInstanceId = "instance-1",
                type = MediaType.APPLICATION_PDF_VALUE,
            )
        val metadataPart =
            MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(metadata),
            )
        val filePart =
            MockMultipartFile(
                "file",
                "upload.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                byteArrayOf(4, 5, 6),
            )
        whenever(fileService.put(any(), any())).thenReturn(fileId)

        mockMvc
            .perform(multipart(PATH).file(metadataPart).file(filePart))
            .andExpect(status().isCreated)
            .andExpect(content().string(objectMapper.writeValueAsString(fileId)))

        val fileCaptor = argumentCaptor<FilePayload>()
        verify(fileService).put(any(), fileCaptor.capture())
        assertThat(fileCaptor.firstValue.name).isEqualTo("document.pdf")
        assertThat(fileCaptor.firstValue.sourceApplicationId).isEqualTo(123L)
        assertThat(fileCaptor.firstValue.sourceApplicationInstanceId).isEqualTo("instance-1")
        assertThat(fileCaptor.firstValue.type).isEqualTo(MediaType.APPLICATION_PDF)
        assertThat(fileCaptor.firstValue.encoding).isEqualTo("binary")
        assertThat(fileCaptor.firstValue.contents).containsExactly(4, 5, 6)
    }

    private companion object {
        private const val PATH = "/api/intern-klient/filer"
    }
}
