package no.fintlabs;

import no.fintlabs.model.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
}
