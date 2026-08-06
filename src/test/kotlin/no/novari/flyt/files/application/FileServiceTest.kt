package no.novari.flyt.files.application

import no.novari.flyt.files.domain.FileDownload
import no.novari.flyt.files.domain.FileMetadata
import no.novari.flyt.files.domain.FilePayload
import no.novari.flyt.files.domain.exception.FileNotFoundException
import no.novari.flyt.files.infrastructure.storage.FileRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import java.io.ByteArrayInputStream
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class FileServiceTest {
    @Mock
    private lateinit var fileRepository: FileRepository

    @InjectMocks
    private lateinit var fileService: FileService

    private lateinit var file: FilePayload
    private lateinit var fileId: UUID
    private lateinit var fileIds: List<UUID>

    @BeforeEach
    fun setUp() {
        file =
            FilePayload(
                name = "example.pdf",
                sourceApplicationId = 123L,
                sourceApplicationInstanceId = "instance-1",
                type = MediaType.APPLICATION_PDF,
                encoding = "base64",
                contents = byteArrayOf(1, 2, 3),
            )
        fileId = UUID.fromString("c4f18f8e-3187-462b-80ea-70f77d00d5b5")
        fileIds =
            listOf(
                UUID.fromString("d197a1fb-7c4f-4ab0-8f38-df32c6c34ed9"),
                UUID.fromString("0c56141b-d8f0-4988-9d09-61bcc4fbbb29"),
                UUID.fromString("201eb809-3acb-4dae-9433-019cd6bf49fe"),
            )
    }

    @Test
    fun `openDownload returns repository download`() {
        val contents = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val fileDownload = FileDownload(file.toMetadata(), contents)
        whenever(fileRepository.openDownload(fileId)).thenReturn(fileDownload)

        val result = fileService.openDownload(fileId)

        assertThat(result.metadata).isEqualTo(file.toMetadata())
        assertThat(result.contents).isSameAs(contents)
        verify(fileRepository, times(1)).openDownload(fileId)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `openDownload normalizes metadata file name`() {
        val contents = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val fileDownload =
            FileDownload(
                metadata = file.toMetadata().copy(name = "  Example document – applicant copy .pdf  "),
                contents = contents,
            )
        whenever(fileRepository.openDownload(fileId)).thenReturn(fileDownload)

        val result = fileService.openDownload(fileId)

        assertThat(result.metadata.name).isEqualTo("Example document - applicant copy.pdf")
        assertThat(result.contents).isSameAs(contents)
        verify(fileRepository, times(1)).openDownload(fileId)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `openDownload normalizes metadata file name to nfc`() {
        val contents = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val fileDownload =
            FileDownload(
                metadata = file.toMetadata().copy(name = "lønns arb vilkår st olav rog fylkl.pdf"),
                contents = contents,
            )
        whenever(fileRepository.openDownload(fileId)).thenReturn(fileDownload)

        val result = fileService.openDownload(fileId)

        assertThat(result.metadata.name).isEqualTo("lønns arb vilkår st olav rog fylkl.pdf")
        assertThat(result.contents).isSameAs(contents)
        verify(fileRepository, times(1)).openDownload(fileId)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `openDownload throws FileNotFoundException when file is missing`() {
        whenever(fileRepository.openDownload(fileId)).thenReturn(null)

        assertThrows<FileNotFoundException> {
            fileService.openDownload(fileId)
        }

        verify(fileRepository, times(1)).openDownload(fileId)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `openDownload propagates repository error`() {
        whenever(fileRepository.openDownload(fileId)).thenThrow(RuntimeException::class.java)

        assertThrows<RuntimeException> {
            fileService.openDownload(fileId)
        }

        verify(fileRepository, times(1)).openDownload(fileId)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `put stores file in repository`() {
        whenever(fileRepository.putFile(fileId, file)).thenReturn(fileId)

        val result = fileService.put(fileId, file)

        assertThat(result).isEqualTo(fileId)
        verify(fileRepository, times(1)).putFile(fileId, file)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `put normalizes file name to nfc before storing`() {
        val decomposedFile = file.copy(name = "lønns arb vilkår st olav rog fylkl.pdf")
        val storedFileCaptor = argumentCaptor<FilePayload>()
        whenever(fileRepository.putFile(eq(fileId), storedFileCaptor.capture())).thenReturn(fileId)

        val result = fileService.put(fileId, decomposedFile)

        assertThat(result).isEqualTo(fileId)
        assertThat(storedFileCaptor.firstValue.name).isEqualTo("lønns arb vilkår st olav rog fylkl.pdf")
        verify(fileRepository, times(1)).putFile(fileId, storedFileCaptor.firstValue)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `put trims file name removes whitespace before extension and normalizes typographic dash`() {
        val fileWithWhitespace = file.copy(name = "  Example document – applicant copy .pdf  ")
        val storedFileCaptor = argumentCaptor<FilePayload>()
        whenever(fileRepository.putFile(eq(fileId), storedFileCaptor.capture())).thenReturn(fileId)

        val result = fileService.put(fileId, fileWithWhitespace)

        assertThat(result).isEqualTo(fileId)
        assertThat(storedFileCaptor.firstValue.name).isEqualTo("Example document - applicant copy.pdf")
        verify(fileRepository, times(1)).putFile(fileId, storedFileCaptor.firstValue)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `put propagates repository error`() {
        whenever(fileRepository.putFile(fileId, file)).thenThrow(RuntimeException::class.java)

        assertThrows<RuntimeException> {
            fileService.put(fileId, file)
        }

        verify(fileRepository, times(1)).putFile(fileId, file)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `delete delegates to repository`() {
        fileService.delete(fileIds)

        verify(fileRepository, times(1)).deleteFiles(fileIds)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `delete ignores empty fileIds`() {
        fileService.delete(emptyList())

        verifyNoInteractions(fileRepository)
    }

    @Test
    fun `delete propagates repository error`() {
        whenever(fileRepository.deleteFiles(fileIds)).thenThrow(RuntimeException::class.java)

        assertThrows<RuntimeException> {
            fileService.delete(fileIds)
        }

        verify(fileRepository, times(1)).deleteFiles(fileIds)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `deleteFilesOlderThan returns count from repository`() {
        val days = 10
        val deletedCount = 5
        whenever(fileRepository.deleteFilesOlderThan(days)).thenReturn(deletedCount)

        val result = fileService.deleteFilesOlderThan(days)

        assertThat(result).isEqualTo(deletedCount)
        verify(fileRepository, times(1)).deleteFilesOlderThan(days)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `deleteFilesOlderThan propagates repository error`() {
        val days = 15
        whenever(fileRepository.deleteFilesOlderThan(days)).thenThrow(RuntimeException("Deletion error"))

        assertThrows<RuntimeException> {
            fileService.deleteFilesOlderThan(days)
        }

        verify(fileRepository, times(1)).deleteFilesOlderThan(days)
        verifyNoMoreInteractions(fileRepository)
    }

    private fun FilePayload.toMetadata(): FileMetadata {
        return FileMetadata(
            name = name,
            sourceApplicationId = sourceApplicationId,
            sourceApplicationInstanceId = sourceApplicationInstanceId,
            type = type,
            encoding = encoding,
        )
    }
}
