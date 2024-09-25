package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.flyt.kafka.event.InstanceFlowEventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@AllArgsConstructor
@Slf4j
public class InstanceDeletedConsumerConfiguration {

    private final EventTopicService eventTopicService;
    private final FileService fileService;

    @Bean
    public ConcurrentMessageListenerContainer<String, Object>
    prepareInstanceToDispatchEventConsumer(InstanceFlowEventConsumerFactoryService instanceFlowEventConsumerFactoryService) {
        EventTopicNameParameters topic = EventTopicNameParameters.builder()
                .eventName("instance-deleted")
                .build();

        eventTopicService.ensureTopic(topic, 0);

        return instanceFlowEventConsumerFactoryService.createRecordFactory(
                Object.class,
                instanceFlowConsumerRecord -> fileService.cleanupFiles(
                        instanceFlowConsumerRecord.getInstanceFlowHeaders()
                )
        ).createContainer(topic);
    }

}
