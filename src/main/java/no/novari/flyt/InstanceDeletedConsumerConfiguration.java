package no.novari.flyt;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.novari.flyt.file.FileService;
import no.fintlabs.flyt.kafka.instanceflow.consuming.InstanceFlowListenerFactoryService;
import no.fintlabs.flyt.kafka.instanceflow.headers.InstanceFlowHeaders;
import no.fintlabs.kafka.consuming.ErrorHandlerConfiguration;
import no.fintlabs.kafka.consuming.ErrorHandlerFactory;
import no.fintlabs.kafka.consuming.ListenerConfiguration;
import no.fintlabs.kafka.topic.name.EventTopicNameParameters;
import no.fintlabs.kafka.topic.name.TopicNamePrefixParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@AllArgsConstructor
@Slf4j
public class InstanceDeletedConsumerConfiguration {

    private final FileService fileService;

    @Bean
    public ConcurrentMessageListenerContainer<String, Object>
    handleInstanceDeletedEvent(
            InstanceFlowListenerFactoryService instanceFlowListenerFactoryService,
            ErrorHandlerFactory errorHandlerFactory
    ) {
        EventTopicNameParameters topic = EventTopicNameParameters
                .builder()
                .topicNamePrefixParameters(TopicNamePrefixParameters
                        .builder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build()
                )
                .eventName("instance-deleted")
                .build();

        return instanceFlowListenerFactoryService.createRecordListenerContainerFactory(
                Object.class,
                instanceFlowConsumerRecord -> {
                    InstanceFlowHeaders instanceFlowHeaders = instanceFlowConsumerRecord.getInstanceFlowHeaders();
                    log.info(generateLogMessageRelatedToInstanceFlowHeaders("Deleting", instanceFlowHeaders));
                    fileService.delete(instanceFlowHeaders.getFileIds())
                            .doOnSuccess(aVoid -> log.info(generateLogMessageRelatedToInstanceFlowHeaders(
                                    "Successfully deleted",
                                    instanceFlowHeaders
                            )))
                            .doOnError(throwable -> log.error(generateLogMessageRelatedToInstanceFlowHeaders(
                                    "Could not delete",
                                    instanceFlowHeaders
                            )))
                            .block();
                },
                ListenerConfiguration
                        .stepBuilder()
                        .groupIdApplicationDefault()
                        .maxPollRecordsKafkaDefault()
                        .maxPollIntervalKafkaDefault()
                        .seekToBeginningOnAssignment()
                        .build(),
                errorHandlerFactory.createErrorHandler(
                        ErrorHandlerConfiguration
                                .stepBuilder()
                                .noRetries()
                                .skipFailedRecords()
                                .build()
                )
        ).createContainer(topic);
    }

    private String generateLogMessageRelatedToInstanceFlowHeaders(String action, InstanceFlowHeaders instanceFlowHeaders) {
        return String.format("%s file(s) related to instance flow with headers=%s", action, instanceFlowHeaders);
    }
}