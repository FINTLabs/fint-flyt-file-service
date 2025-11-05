package no.fintlabs;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import no.fintlabs.file.DeletedFile;
import no.fintlabs.file.File;
import no.fintlabs.file.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileRepositoryTest {

    @Mock
    private AzureBlobAdapter azureBlobAdapter;

    @InjectMocks
    private FileRepository fileRepository;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Logger logger = (Logger) LoggerFactory.getLogger(FileRepository.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    void putFileTest() {
        UUID fileId = UUID.randomUUID();
        File file = mock(File.class);
        when(file.getName()).thenReturn("testName");
        when(azureBlobAdapter.uploadFile(eq(fileId), eq(file))).thenReturn(Mono.just(fileId));

        StepVerifier.create(fileRepository.putFile(fileId, file))
                .expectNext(fileId)
                .verifyComplete();

        verify(azureBlobAdapter).uploadFile(fileId, file);
        assertThat(listAppender.list)
                .anyMatch(event -> event.getFormattedMessage()
                        .contains("Successfully uploaded File{fileId=" + fileId + "}"));
    }

    @Test
    void findByIdTest() {
        UUID fileId = UUID.randomUUID();
        File file = mock(File.class);
        Optional<File> optionalFile = Optional.of(file);

        when(azureBlobAdapter.downloadFile(eq(fileId))).thenReturn(Mono.just(optionalFile));

        StepVerifier.create(fileRepository.findById(fileId))
                .expectNextMatches(result -> result.isPresent() && result.get().equals(file))
                .verifyComplete();

        verify(azureBlobAdapter).downloadFile(fileId);
        assertThat(listAppender.list)
                .anyMatch(event -> event.getFormattedMessage()
                        .contains("Successfully found File{fileId=" + fileId + "}"));
    }

    @Test
    void deleteFilesTest() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> ids = Arrays.asList(id1, id2);

        when(azureBlobAdapter.deleteFilesByIds(eq(ids))).thenReturn(Mono.empty());

        StepVerifier.create(fileRepository.deleteFiles(ids))
                .verifyComplete();

        verify(azureBlobAdapter).deleteFilesByIds(ids);
        assertThat(listAppender.list)
                .anyMatch(event -> event.getFormattedMessage()
                        .contains("Successfully deleted File{fileId=" + id1 + "}"));
        assertThat(listAppender.list)
                .anyMatch(event -> event.getFormattedMessage()
                        .contains("Successfully deleted File{fileId=" + id2 + "}"));
    }

    @Test
    void deleteFilesOlderThanTest() {
        int days = 30;
        OffsetDateTime now = OffsetDateTime.now();
        DeletedFile df1 = new DeletedFile("fileA.txt", now.minusDays(40));
        DeletedFile df2 = new DeletedFile("fileB.txt", now.minusDays(50));
        List<DeletedFile> list = Arrays.asList(df1, df2);

        when(azureBlobAdapter.deleteFilesOlderThanDays(eq(days))).thenReturn(list);

        int count = fileRepository.deleteFilesOlderThan(days);

        assertThat(count).isEqualTo(2);
        assertThat(listAppender.list).anyMatch(event ->
                event.getFormattedMessage().contains("deleted file with name fileA.txt, timestamp " + df1.deletedAt()));
        assertThat(listAppender.list).anyMatch(event ->
                event.getFormattedMessage().contains("deleted file with name fileB.txt, timestamp " + df2.deletedAt()));
    }
}
