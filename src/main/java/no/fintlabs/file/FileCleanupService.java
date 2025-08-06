package no.fintlabs.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileCleanupService {

    @Value("${fint.flyt.file-service.time-to-keep-azure-blobs-in-days:180}")
    private int timeToKeepAzureBlobsInDays;

    private final FileService fileService;

    @Scheduled(initialDelay = 30000, fixedDelay = 86400000)
    public void cleanup() {
        log.info("Cleaning up azure blobs older than {} days", timeToKeepAzureBlobsInDays);
        var numberOfDeletedFiles = fileService.deleteFilesOlderThan(timeToKeepAzureBlobsInDays);
        log.info("Number of deleted files {}", numberOfDeletedFiles);
    }

}
