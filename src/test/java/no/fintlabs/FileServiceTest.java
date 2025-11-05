package no.fintlabs;

import com.google.common.collect.ImmutableList;
import no.fintlabs.cache.FintCache;
import no.fintlabs.file.File;
import no.fintlabs.file.FileRepository;
import no.fintlabs.file.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FileServiceTest {

    @Mock
    private FintCache<UUID, File> fileCache;

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    private File file;

    private UUID fileId;
    private ImmutableList<UUID> fileIds;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        file = mock(File.class);
        fileId = UUID.fromString("c4f18f8e-3187-462b-80ea-70f77d00d5b5");
        fileIds = ImmutableList.of(
                UUID.fromString("d197a1fb-7c4f-4ab0-8f38-df32c6c34ed9"),
                UUID.fromString("0c56141b-d8f0-4988-9d09-61bcc4fbbb29"),
                UUID.fromString("201eb809-3acb-4dae-9433-019cd6bf49fe")
        );
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

    @Test
    void givenFileIdAndFileWhenPutFileShouldPutInCacheAndRepository() {
        when(fileRepository.putFile(fileId, file)).thenReturn(Mono.just(fileId));

        StepVerifier.create(fileService.put(fileId, file))
                .expectNext(fileId)
                .verifyComplete();

        verify(fileCache, times(1)).put(fileId, file);
        verifyNoMoreInteractions(fileCache);

        verify(fileRepository, times(1)).putFile(fileId, file);
        verifyNoMoreInteractions(fileRepository);
    }


    @Test
    void givenNullFileIdAndFileWhenPutShouldReturnMonoWithError() {
        StepVerifier.create(fileService.put(null, file))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && "FileId cannot be null".equals(throwable.getMessage())
                )
                .verify();
    }

    @Test
    void givenFileIdAndNullFileWhenPutShouldReturnMonoWithError() {
        StepVerifier.create(fileService.put(fileId, null))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && "File cannot be null".equals(throwable.getMessage())
                )
                .verify();
    }

    @Test
    void givenNullFileIdAndNullFileWhenPutShouldReturnMonoWithError() {
        StepVerifier.create(fileService.put(null, null))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && "FileId cannot be null".equals(throwable.getMessage())
                )
                .verify();
    }

    @Test
    void givenExceptionFromFileCacheWhenPutShouldReturnMonoWithError() {
        doThrow(RuntimeException.class).when(fileCache).put(fileId, file);

        StepVerifier.create(fileService.put(fileId, file))
                .expectError(RuntimeException.class)
                .verify();

        verify(fileCache, times(1)).put(fileId, file);
        verifyNoMoreInteractions(fileCache);

        verifyNoInteractions(fileRepository);
    }

    @Test
    void givenSuccessFromFileCacheAndExceptionFromFileRepositoryWhenPutFileShouldRemoveFromCacheAndReturnMonoWithError() {
        when(fileRepository.putFile(fileId, file)).thenThrow(RuntimeException.class);

        StepVerifier.create(fileService.put(fileId, file))
                .expectError(RuntimeException.class)
                .verify();

        verify(fileCache, times(1)).put(fileId, file);
        verify(fileCache, times(1)).remove(fileId);
        verifyNoMoreInteractions(fileCache);

        verify(fileRepository, times(1)).putFile(fileId, file);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void givenSuccessFromFileCacheAndMonoWithErrorFromFileRepositoryWhenPutFileShouldRemoveFromCacheAndReturnMonoWithError() {
        when(fileRepository.putFile(fileId, file)).thenReturn(Mono.error(new RuntimeException()));

        StepVerifier.create(fileService.put(fileId, file))
                .expectError(RuntimeException.class)
                .verify();

        verify(fileCache, times(1)).put(fileId, file);
        verify(fileCache, times(1)).remove(fileId);
        verifyNoMoreInteractions(fileCache);

        verify(fileRepository, times(1)).putFile(fileId, file);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void givenFileIdsWhenDeleteShouldInvokeDeleteOnFileCacheAndFileRepository() {
        when(fileRepository.deleteFiles(fileIds)).thenReturn(Mono.empty());

        StepVerifier.create(fileService.delete(fileIds))
                .expectNoAccessibleContext()
                .verifyComplete();

        verify(fileCache, times(1)).remove(fileIds);
        verifyNoMoreInteractions(fileCache);
        verify(fileRepository, times(1)).deleteFiles(fileIds);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void givenEmptyFileIdsShouldReturnWithoutInvokingFileCacheAndFileRepository() {
        StepVerifier.create(fileService.delete(List.of()))
                .expectNoAccessibleContext()
                .verifyComplete();

        verifyNoInteractions(fileCache);
        verifyNoInteractions(fileRepository);
    }

    @Test
    void givenNullFileIdsShouldReturnWithoutInvokingFileCacheAndFileRepository() {
        StepVerifier.create(fileService.delete(null))
                .expectNoAccessibleContext()
                .verifyComplete();

        verifyNoInteractions(fileCache);
        verifyNoInteractions(fileRepository);
    }

    @Test
    void givenExceptionFromFileCacheWhenDeleteShouldReturnMonoWithError() {
        doThrow(RuntimeException.class).when(fileCache).remove(fileIds);

        StepVerifier.create(fileService.delete(fileIds))
                .expectError(RuntimeException.class)
                .verify();

        verify(fileCache, times(1)).remove(fileIds);
        verifyNoMoreInteractions(fileCache);
        verifyNoInteractions(fileRepository);
    }

    @Test
    void givenExceptionFromFileRepositoryWhenDeleteShouldReturnMonoWithError() {
        when(fileRepository.deleteFiles(fileIds)).thenThrow(RuntimeException.class);

        StepVerifier.create(fileService.delete(fileIds))
                .expectError(RuntimeException.class)
                .verify();

        verify(fileCache, times(1)).remove(fileIds);
        verifyNoMoreInteractions(fileCache);
        verify(fileRepository, times(1)).deleteFiles(fileIds);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void givenMonoWithErrorFromFileRepositoryWhenDeleteShouldReturnMonoWithError() {
        when(fileRepository.deleteFiles(fileIds)).thenReturn(Mono.error(new RuntimeException()));

        StepVerifier.create(fileService.delete(fileIds))
                .expectError(RuntimeException.class)
                .verify();

        verify(fileCache, times(1)).remove(fileIds);
        verifyNoMoreInteractions(fileCache);
        verify(fileRepository, times(1)).deleteFiles(fileIds);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void deleteFilesOlderThanShouldReturnCountFromRepository() {
        int days = 10;
        int deletedCount = 5;
        when(fileRepository.deleteFilesOlderThan(days)).thenReturn(deletedCount);

        int result = fileService.deleteFilesOlderThan(days);

        assertThat(result).isEqualTo(deletedCount);
        verify(fileRepository, times(1)).deleteFilesOlderThan(days);
        verifyNoInteractions(fileCache);
    }

    @Test
    void deleteFilesOlderThanWithExceptionShouldPropagateException() {
        int days = 15;
        when(fileRepository.deleteFilesOlderThan(days)).thenThrow(new RuntimeException("Deletion error"));

        assertThrows(RuntimeException.class, () -> fileService.deleteFilesOlderThan(days));
        verify(fileRepository, times(1)).deleteFilesOlderThan(days);
        verifyNoInteractions(fileCache);
    }

}