package no.fintlabs;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

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
        Optional<File> optionalFile = Optional.of(file);

        when(azureBlobAdapter.downloadFile(eq(fileId))).thenReturn(Mono.just(optionalFile));

        StepVerifier.create(fileRepository.findById(fileId))
                .expectNextMatches(result -> result.isPresent() && result.get().equals(file))
                .verifyComplete();

        verify(azureBlobAdapter).downloadFile(fileId);
    }

}
