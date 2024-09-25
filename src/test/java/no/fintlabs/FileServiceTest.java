package no.fintlabs;

import no.fintlabs.cache.FintCache;
import no.fintlabs.model.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class FileServiceTest {

    @Mock
    private FintCache<UUID, File> fileCache;

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    private File file;

    private UUID fileId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        file = File.builder()
                .name("testFile.txt")
                .sourceApplicationId(123L)
                .sourceApplicationInstanceId("instance_001")
                .type(MediaType.TEXT_PLAIN)
                .encoding("UTF-8")
                .contents("sample-content".getBytes())
                .build();

        fileId = UUID.randomUUID();
    }

    @Test
    void givenFileExistsInCacheWhenFindByIdShouldGetFileFromCacheAndReturn() {
        when(fileCache.getOptional(fileId)).thenReturn(Optional.of(file));

        StepVerifier.create(fileService.findById(fileId))
                .expectNext(Optional.of(file))
                .verifyComplete();

        verify(fileCache, times(1)).getOptional(fileId);
        verifyNoMoreInteractions(fileCache);
        verifyNoInteractions(fileRepository);
    }

    @Test
    void givenFileDoesNotExistInCacheAndExistsInRepositoryWhenFindByIdShouldGetFileFromRepositoryAndReturn() {
        when(fileRepository.findById(fileId)).thenReturn(Mono.just(Optional.of(file)));

        StepVerifier.create(fileService.findById(fileId))
                .expectNext(Optional.of(file))
                .verifyComplete();

        verify(fileCache, times(1)).getOptional(fileId);
        verifyNoMoreInteractions(fileCache);
        verify(fileRepository, times(1)).findById(fileId);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void givenFileDoesNotExistInCacheAndRepositoryWhenFindByIdShouldReturnEmptyOptional() {
        when(fileRepository.findById(fileId)).thenReturn(Mono.just(Optional.empty()));

        StepVerifier.create(fileService.findById(fileId))
                .expectNext(Optional.empty())
                .verifyComplete();

        verify(fileCache, times(1)).getOptional(fileId);
        verifyNoMoreInteractions(fileCache);
        verify(fileRepository, times(1)).findById(fileId);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void givenExceptionFromFileCacheWhenFindByIdShouldReturnMonoWithError() {
        when(fileCache.getOptional(fileId)).thenThrow(RuntimeException.class);

        StepVerifier.create(fileService.findById(fileId))
                .expectError(RuntimeException.class)
                .verify();

        verify(fileCache, times(1)).getOptional(fileId);
        verifyNoMoreInteractions(fileCache);
    }

    @Test
    void givenExceptionFromFileRepositoryWhenFindByIdShouldReturnMonoWithError() {
        when(fileRepository.findById(fileId)).thenThrow(RuntimeException.class);

        StepVerifier.create(fileService.findById(fileId))
                .expectError(RuntimeException.class)
                .verify();

        verify(fileCache, times(1)).getOptional(fileId);
        verifyNoMoreInteractions(fileCache);
        verify(fileRepository, times(1)).findById(fileId);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void givenMonoExceptionFromFileRepositoryWhenFindByIdShouldReturnMonoWithError() {
        when(fileRepository.findById(fileId)).thenReturn(Mono.error(new RuntimeException()));

        StepVerifier.create(fileService.findById(fileId))
                .expectError(RuntimeException.class)
                .verify();

        verify(fileCache, times(1)).getOptional(fileId);
        verifyNoMoreInteractions(fileCache);
        verify(fileRepository, times(1)).findById(fileId);
        verifyNoMoreInteractions(fileRepository);
    }


//
//    @Test
//    void testAddFile() {
//        when(fileRepository.putFile(any(UUID.class), eq(file))).thenReturn(Mono.just(fileId));
//
//        Mono<ResponseEntity<UUID>> resultMono = fileController.addFile(Mono.just(file));
//
//        StepVerifier.create(resultMono)
//                .expectNextMatches(response ->
//                        response.getStatusCode().equals(HttpStatus.CREATED) && response.getBody() != null)
//                .verifyComplete();
//
//        verify(fileCache).put(any(UUID.class), eq(file));
//    }
//
//    @Test
//    public void testAddFile_InvalidInput() {
//        File invalidFile = File.builder().build();
//
//        Mono<ResponseEntity<UUID>> resultMono = fileController.addFile(Mono.just(invalidFile));
//
//        StepVerifier.create(resultMono)
//                .expectErrorMatches(throwable ->
//                        throwable instanceof RuntimeException)
//                .verify();
//    }
//
//    @Test
//    public void testAddFile_RepositoryError() {
//        File someFile = File.builder().build();
//
//        when(fileRepository.putFile(any(UUID.class), eq(someFile)))
//                .thenReturn(Mono.error(new RuntimeException("Repository error"))); // You can replace RuntimeException with a more specific error if you wish.
//
//        Mono<ResponseEntity<UUID>> resultMono = fileController.addFile(Mono.just(someFile));
//
//        StepVerifier.create(resultMono)
//                .expectErrorMatches(throwable ->
//                        throwable instanceof RuntimeException &&
//                                throwable.getMessage().contains("Repository error"))
//                .verify();
//    }

}
