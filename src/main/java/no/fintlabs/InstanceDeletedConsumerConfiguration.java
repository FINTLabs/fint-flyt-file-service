package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.flyt.kafka.event.InstanceFlowEventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.List;
import java.util.UUID;

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
                    List<UUID> fileIds = instanceFlowConsumerRecord.getInstanceFlowHeaders().getFileIds();
                    fileService.delete(fileIds)
                            .doOnError(throwable -> log.error(
                                    "Could not delete file(s) related to instance flow with headers={}",
                                    instanceFlowConsumerRecord.getInstanceFlowHeaders()
                            ))
                            .block();
                }
        ).createContainer(topic);
    }

}