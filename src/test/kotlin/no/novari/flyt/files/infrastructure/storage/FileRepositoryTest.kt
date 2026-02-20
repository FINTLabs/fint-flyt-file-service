package no.novari.flyt.files.infrastructure.storage

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.novari.flyt.files.domain.DeletedFile
import no.novari.flyt.files.domain.FilePayload
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class FileRepositoryTest {
    @Mock
    private lateinit var blobStorageAdapter: BlobStorageAdapter

    @InjectMocks
    private lateinit var fileRepository: FileRepository

    private lateinit var listAppender: ListAppender<ILoggingEvent>

    @BeforeEach
    fun setUp() {
        val logger = LoggerFactory.getLogger(FileRepository::class.java) as Logger
        listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        logger.addAppender(listAppender)
    }

    @Test
    fun `putFile uploads file and logs success`() {
        val fileId = UUID.randomUUID()
        val file = mock<FilePayload>()
        whenever(blobStorageAdapter.uploadFile(eq(fileId), eq(file))).thenReturn(fileId)

        val result = fileRepository.putFile(fileId, file)

        assertThat(result).isEqualTo(fileId)
        verify(blobStorageAdapter).uploadFile(fileId, file)
        assertThat(listAppender.list)
            .anyMatch { event ->
                event.formattedMessage.contains("Successfully uploaded File{fileId=$fileId}")
            }
    }

    @Test
    fun `findById returns file and logs success`() {
        val fileId = UUID.randomUUID()
        val file = mock<FilePayload>()

        whenever(blobStorageAdapter.downloadFile(eq(fileId))).thenReturn(file)

        val result = fileRepository.findById(fileId)

        assertThat(result).isEqualTo(file)
        verify(blobStorageAdapter).downloadFile(fileId)
        assertThat(listAppender.list)
            .anyMatch { event ->
                event.formattedMessage.contains("Successfully found File{fileId=$fileId}")
            }
    }

    @Test
    fun `deleteFiles deletes all ids and logs success`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val ids = listOf(id1, id2)

        fileRepository.deleteFiles(ids)

        verify(blobStorageAdapter).deleteFilesByIds(ids)
        assertThat(listAppender.list)
            .anyMatch { event ->
                event.formattedMessage.contains("Successfully deleted File{fileId=$id1}")
            }
        assertThat(listAppender.list)
            .anyMatch { event ->
                event.formattedMessage.contains("Successfully deleted File{fileId=$id2}")
            }
    }

    @Test
    fun `deleteFilesOlderThan returns count and logs deleted files`() {
        val days = 30
        val now = OffsetDateTime.now()
        val deletedFile1 = DeletedFile("fileA.txt", now.minusDays(40))
        val deletedFile2 = DeletedFile("fileB.txt", now.minusDays(50))
        val deletedFiles = listOf(deletedFile1, deletedFile2)

        whenever(blobStorageAdapter.deleteFilesOlderThanDays(eq(days))).thenReturn(deletedFiles)

        val count = fileRepository.deleteFilesOlderThan(days)

        assertThat(count).isEqualTo(2)
        assertThat(listAppender.list).anyMatch { event ->
            event.formattedMessage.contains("deleted file with name fileA.txt, timestamp ${deletedFile1.deletedAt}")
        }
        assertThat(listAppender.list).anyMatch { event ->
            event.formattedMessage.contains("deleted file with name fileB.txt, timestamp ${deletedFile2.deletedAt}")
        }
    }
}
