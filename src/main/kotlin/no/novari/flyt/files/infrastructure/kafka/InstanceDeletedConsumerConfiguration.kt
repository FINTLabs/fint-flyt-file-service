package no.novari.flyt.files.infrastructure.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.files.application.FileService
import no.novari.flyt.kafka.instanceflow.consuming.InstanceFlowListenerFactoryService
import no.novari.kafka.consuming.ErrorHandlerConfiguration
import no.novari.kafka.consuming.ErrorHandlerFactory
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@Configuration
class InstanceDeletedConsumerConfiguration(
    private val fileService: FileService,
) {
    private val log = KotlinLogging.logger {}

    @Bean
    fun handleInstanceDeletedEvent(
        instanceFlowListenerFactoryService: InstanceFlowListenerFactoryService,
        errorHandlerFactory: ErrorHandlerFactory,
    ): ConcurrentMessageListenerContainer<String, Any> {
        val topic =
            EventTopicNameParameters
                .builder()
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters
                        .stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build(),
                ).eventName("instance-deleted")
                .build()

        return instanceFlowListenerFactoryService
            .createRecordListenerContainerFactory(
                Any::class.java,
                { instanceFlowConsumerRecord ->
                    val instanceFlowHeaders = instanceFlowConsumerRecord.instanceFlowHeaders
                    log.atInfo {
                        message = "{} file(s) related to instance flow with headers={}"
                        arguments = arrayOf("Deleting", instanceFlowHeaders)
                    }

                    try {
                        fileService.delete(instanceFlowHeaders.fileIds.orEmpty())
                        log.atInfo {
                            message = "{} file(s) related to instance flow with headers={}"
                            arguments = arrayOf("Successfully deleted", instanceFlowHeaders)
                        }
                    } catch (exception: Exception) {
                        log.atError {
                            message = "{} file(s) related to instance flow with headers={}"
                            arguments = arrayOf("Could not delete", instanceFlowHeaders)
                            cause = exception
                        }
                        throw exception
                    }
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
                        .stepBuilder<Any>()
                        .noRetries()
                        .skipFailedRecords()
                        .build(),
                ),
            ).createContainer(topic)
    }
}
