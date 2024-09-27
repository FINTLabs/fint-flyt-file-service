package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.flyt.kafka.event.InstanceFlowEventConsumerFactoryService;
import no.fintlabs.flyt.kafka.headers.InstanceFlowHeaders;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
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
    handleInstanceDeletedEvent(InstanceFlowEventConsumerFactoryService instanceFlowEventConsumerFactoryService) {
        EventTopicNameParameters topic = EventTopicNameParameters.builder()
                .eventName("instance-deleted")
                .build();
        return instanceFlowEventConsumerFactoryService.createRecordFactory(
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
                }
        ).createContainer(topic);
    }

    private String generateLogMessageRelatedToInstanceFlowHeaders(String action, InstanceFlowHeaders instanceFlowHeaders) {
        return String.format("%s file(s) related to instance flow with headers=%s", action, instanceFlowHeaders);
    }
}