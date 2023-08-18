package no.fintlabs;

import no.fintlabs.model.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class FileRepositoryTest {

    @Mock
    private AzureBlobAdapter azureBlobAdapter;

    @InjectMocks
    private FileRepository fileRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Logger logger = (Logger) LoggerFactory.getLogger(FileRepository.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
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
    }

    @Test
    void findByIdTest() {
        UUID fileId = UUID.randomUUID();
        File file = mock(File.class);
        when(file.getName()).thenReturn("testName");
        when(azureBlobAdapter.downloadFile(eq(fileId))).thenReturn(Mono.just(file));

        StepVerifier.create(fileRepository.findById(fileId))
                .expectNext(file)
                .verifyComplete();

        verify(azureBlobAdapter).downloadFile(fileId);
    }

    @Test
    void deleteByTagsTest() {
        Long sourceApplicationId = 123L;
        String sourceApplicationInstanceId = "instanceId";

        when(azureBlobAdapter.deleteFilesByTags(eq(sourceApplicationId), eq(sourceApplicationInstanceId))).thenReturn(Mono.empty());

        StepVerifier.create(fileRepository.deleteByTags(sourceApplicationId, sourceApplicationInstanceId))
                .verifyComplete();

        verify(azureBlobAdapter).deleteFilesByTags(sourceApplicationId, sourceApplicationInstanceId);
    }

    @Test
    void deleteByTags_ErrorScenario() {
        Long sourceApplicationId = 123L;
        String sourceApplicationInstanceId = "instanceId";
        Exception mockException = new Exception("mock exception");

        when(azureBlobAdapter.deleteFilesByTags(eq(sourceApplicationId), eq(sourceApplicationInstanceId)))
                .thenReturn(Mono.error(mockException));

        Logger logger = (Logger) LoggerFactory.getLogger(FileRepository.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        StepVerifier.create(fileRepository.deleteByTags(sourceApplicationId, sourceApplicationInstanceId))
                .verifyError();

        boolean logMessageFound = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR &&
                        event.getFormattedMessage().contains("Could not delete file with tags sourceApplicationId=123 sourceApplicationInstanceId=instanceId"));

        assertTrue(logMessageFound, "Expected log message not found.");

        logger.detachAppender(listAppender);
    }
}
