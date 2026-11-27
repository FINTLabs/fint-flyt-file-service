package no.novari.flyt.files.infrastructure.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.files.application.FileService
import no.novari.flyt.kafka.instanceflow.headers.INSTANCE_FLOW_HEADERS_KEY
import no.novari.flyt.kafka.instanceflow.headers.InstanceFlowHeaders
import no.novari.kafka.starter.topic.EventTopicName
import no.novari.kafka.starter.topic.KafkaTopics
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import org.springframework.util.backoff.FixedBackOff

@Configuration
class InstanceDeletedConsumerConfiguration {
    @Bean
    fun instanceDeletedTopic(topics: KafkaTopics): EventTopicName = topics.event("instance-deleted")

    /** As in fint-flyt-kafka 7 (`noRetries().skipFailedRecords()`): a failed record is logged and skipped. */
    @Bean
    fun kafkaErrorHandler(): CommonErrorHandler = DefaultErrorHandler(FixedBackOff(0, 0))
}

/** Deletes the files of a deleted instance. The records are tombstones, so only the header is read. */
@Component
class InstanceDeletedListener(
    private val fileService: FileService,
) {
    private val log = KotlinLogging.logger {}

    @KafkaListener(topics = ["#{@instanceDeletedTopic.name}"])
    fun on(
        @Header(INSTANCE_FLOW_HEADERS_KEY) instanceFlowHeaders: InstanceFlowHeaders,
    ) {
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
    }
}
