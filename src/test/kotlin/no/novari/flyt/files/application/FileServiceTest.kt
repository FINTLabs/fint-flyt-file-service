package no.novari.flyt.files.application

import no.novari.cache.FintCache
import no.novari.cache.exceptions.NoSuchCacheEntryException
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
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class FileServiceTest {
    @Mock
    private lateinit var fileCache: FintCache<UUID, FilePayload>

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
    fun `findById returns cached file`() {
        whenever(fileCache.get(fileId)).thenReturn(file)

        val result = fileService.findById(fileId)

        assertThat(result).isEqualTo(file)
        verify(fileCache, times(1)).get(fileId)
        verifyNoMoreInteractions(fileCache)
        verifyNoInteractions(fileRepository)
    }

    @Test
    fun `findById returns repository file on cache miss`() {
        whenever(fileCache.get(fileId)).thenReturn(null)
        whenever(fileRepository.findById(fileId)).thenReturn(file)

        val result = fileService.findById(fileId)

        assertThat(result).isEqualTo(file)
        verify(fileCache, times(1)).get(fileId)
        verifyNoMoreInteractions(fileCache)
        verify(fileRepository, times(1)).findById(fileId)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `findById returns repository file when cache entry does not exist`() {
        whenever(fileCache.get(fileId)).thenThrow(
            NoSuchCacheEntryException("No cache entry with key='$fileId'"),
        )
        whenever(fileRepository.findById(fileId)).thenReturn(file)

        val result = fileService.findById(fileId)

        assertThat(result).isEqualTo(file)
        verify(fileCache, times(1)).get(fileId)
        verifyNoMoreInteractions(fileCache)
        verify(fileRepository, times(1)).findById(fileId)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `findById throws FileNotFoundException when file is missing`() {
        whenever(fileCache.get(fileId)).thenReturn(null)
        whenever(fileRepository.findById(fileId)).thenReturn(null)

        assertThrows<FileNotFoundException> {
            fileService.findById(fileId)
        }
        verify(fileCache, times(1)).get(fileId)
        verifyNoMoreInteractions(fileCache)
        verify(fileRepository, times(1)).findById(fileId)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `findById propagates cache error`() {
        whenever(fileCache.get(fileId)).thenThrow(RuntimeException::class.java)

        assertThrows<RuntimeException> {
            fileService.findById(fileId)
        }

        verify(fileCache, times(1)).get(fileId)
        verifyNoMoreInteractions(fileCache)
    }

    @Test
    fun `findById propagates repository thrown error`() {
        whenever(fileCache.get(fileId)).thenReturn(null)
        whenever(fileRepository.findById(fileId)).thenThrow(RuntimeException::class.java)

        assertThrows<RuntimeException> {
            fileService.findById(fileId)
        }

        verify(fileCache, times(1)).get(fileId)
        verifyNoMoreInteractions(fileCache)
        verify(fileRepository, times(1)).findById(fileId)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `findById propagates repository failure`() {
        whenever(fileCache.get(fileId)).thenReturn(null)
        whenever(fileRepository.findById(fileId)).thenAnswer { throw RuntimeException() }

        assertThrows<RuntimeException> {
            fileService.findById(fileId)
        }

        verify(fileCache, times(1)).get(fileId)
        verifyNoMoreInteractions(fileCache)
        verify(fileRepository, times(1)).findById(fileId)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `put stores file in cache and repository`() {
        whenever(fileRepository.putFile(fileId, file)).thenReturn(fileId)

        val result = fileService.put(fileId, file)

        assertThat(result).isEqualTo(fileId)
        verify(fileCache, times(1)).put(fileId, file)
        verifyNoMoreInteractions(fileCache)
        verify(fileRepository, times(1)).putFile(fileId, file)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `findById normalizes cached file name to nfc`() {
        val decomposedFile = file.copy(name = "lønns arb vilkår st olav rog fylkl.pdf")
        whenever(fileCache.get(fileId)).thenReturn(decomposedFile)

        val result = fileService.findById(fileId)

        assertThat(result.name).isEqualTo("lønns arb vilkår st olav rog fylkl.pdf")
        verify(fileCache, times(1)).get(fileId)
        verifyNoMoreInteractions(fileCache)
        verifyNoInteractions(fileRepository)
    }

    @Test
    fun `findById trims file name removes whitespace before extension and normalizes typographic dash`() {
        val fileWithWhitespace = file.copy(name = "  Example document – applicant copy .pdf  ")
        whenever(fileCache.get(fileId)).thenReturn(fileWithWhitespace)

        val result = fileService.findById(fileId)

        assertThat(result.name).isEqualTo("Example document - applicant copy.pdf")
        verify(fileCache, times(1)).get(fileId)
        verifyNoMoreInteractions(fileCache)
        verifyNoInteractions(fileRepository)
    }

    @Test
    fun `put normalizes file name to nfc before storing`() {
        val decomposedFile = file.copy(name = "lønns arb vilkår st olav rog fylkl.pdf")
        val cachedFileCaptor = argumentCaptor<FilePayload>()
        val storedFileCaptor = argumentCaptor<FilePayload>()
        whenever(fileRepository.putFile(eq(fileId), storedFileCaptor.capture())).thenReturn(fileId)

        val result = fileService.put(fileId, decomposedFile)

        assertThat(result).isEqualTo(fileId)
        verify(fileCache, times(1)).put(eq(fileId), cachedFileCaptor.capture())
        assertThat(cachedFileCaptor.firstValue.name).isEqualTo("lønns arb vilkår st olav rog fylkl.pdf")
        assertThat(storedFileCaptor.firstValue.name).isEqualTo("lønns arb vilkår st olav rog fylkl.pdf")
        verifyNoMoreInteractions(fileCache)
        verify(fileRepository, times(1)).putFile(fileId, storedFileCaptor.firstValue)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `put trims file name removes whitespace before extension and normalizes typographic dash`() {
        val fileWithWhitespace = file.copy(name = "  Example document – applicant copy .pdf  ")
        val cachedFileCaptor = argumentCaptor<FilePayload>()
        val storedFileCaptor = argumentCaptor<FilePayload>()
        whenever(fileRepository.putFile(eq(fileId), storedFileCaptor.capture())).thenReturn(fileId)

        val result = fileService.put(fileId, fileWithWhitespace)

        assertThat(result).isEqualTo(fileId)
        verify(fileCache, times(1)).put(eq(fileId), cachedFileCaptor.capture())
        assertThat(cachedFileCaptor.firstValue.name).isEqualTo("Example document - applicant copy.pdf")
        assertThat(storedFileCaptor.firstValue.name).isEqualTo("Example document - applicant copy.pdf")
        verify(fileRepository, times(1)).putFile(fileId, storedFileCaptor.firstValue)
        verifyNoMoreInteractions(fileCache)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `put propagates cache error`() {
        doThrow(RuntimeException::class).whenever(fileCache).put(fileId, file)

        assertThrows<RuntimeException> {
            fileService.put(fileId, file)
        }

        verify(fileCache, times(1)).put(fileId, file)
        verifyNoMoreInteractions(fileCache)
        verifyNoInteractions(fileRepository)
    }

    @Test
    fun `put removes cache entry when repository throws`() {
        whenever(fileRepository.putFile(fileId, file)).thenThrow(RuntimeException::class.java)

        assertThrows<RuntimeException> {
            fileService.put(fileId, file)
        }

        verify(fileCache, times(1)).put(fileId, file)
        verify(fileCache, times(1)).remove(fileId)
        verifyNoMoreInteractions(fileCache)
        verify(fileRepository, times(1)).putFile(fileId, file)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `put removes cache entry when repository fails`() {
        whenever(fileRepository.putFile(fileId, file)).thenAnswer { throw RuntimeException() }

        assertThrows<RuntimeException> {
            fileService.put(fileId, file)
        }

        verify(fileCache, times(1)).put(fileId, file)
        verify(fileCache, times(1)).remove(fileId)
        verifyNoMoreInteractions(fileCache)
        verify(fileRepository, times(1)).putFile(fileId, file)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `delete removes files from cache and repository`() {
        fileService.delete(fileIds)

        verify(fileCache, times(1)).remove(fileIds)
        verifyNoMoreInteractions(fileCache)
        verify(fileRepository, times(1)).deleteFiles(fileIds)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `delete ignores empty fileIds`() {
        fileService.delete(emptyList())

        verifyNoInteractions(fileCache)
        verifyNoInteractions(fileRepository)
    }

    @Test
    fun `delete propagates cache error`() {
        doThrow(RuntimeException::class).whenever(fileCache).remove(fileIds)

        assertThrows<RuntimeException> {
            fileService.delete(fileIds)
        }

        verify(fileCache, times(1)).remove(fileIds)
        verifyNoMoreInteractions(fileCache)
        verifyNoInteractions(fileRepository)
    }

    @Test
    fun `delete propagates repository thrown error`() {
        whenever(fileRepository.deleteFiles(fileIds)).thenThrow(RuntimeException::class.java)

        assertThrows<RuntimeException> {
            fileService.delete(fileIds)
        }

        verify(fileCache, times(1)).remove(fileIds)
        verifyNoMoreInteractions(fileCache)
        verify(fileRepository, times(1)).deleteFiles(fileIds)
        verifyNoMoreInteractions(fileRepository)
    }

    @Test
    fun `delete propagates repository failure`() {
        whenever(fileRepository.deleteFiles(fileIds)).thenAnswer { throw RuntimeException() }

        assertThrows<RuntimeException> {
            fileService.delete(fileIds)
        }

        verify(fileCache, times(1)).remove(fileIds)
        verifyNoMoreInteractions(fileCache)
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
        verifyNoInteractions(fileCache)
    }

    @Test
    fun `deleteFilesOlderThan propagates repository error`() {
        val days = 15
        whenever(fileRepository.deleteFilesOlderThan(days)).thenThrow(RuntimeException("Deletion error"))

        assertThrows<RuntimeException> {
            fileService.deleteFilesOlderThan(days)
        }

        verify(fileRepository, times(1)).deleteFilesOlderThan(days)
        verifyNoInteractions(fileCache)
    }
}
