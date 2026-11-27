package no.novari.flyt.files.infrastructure.kafka

import no.novari.Application
import no.novari.flyt.files.application.FileService
import no.novari.flyt.kafka.instanceflow.headers.InstanceFlowHeaders
import no.novari.flyt.kafka.instanceflow.producing.send
import no.novari.kafka.starter.topic.EventTopicName
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

@SpringBootTest(
    classes = [Application::class],
    properties = ["spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}"],
)
@EmbeddedKafka(topics = ["novari-no.flyt.event.instance-deleted"])
@ActiveProfiles("local-staging")
class InstanceDeletedListenerTest {
    @MockitoBean
    private lateinit var fileService: FileService

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var instanceDeletedTopic: EventTopicName

    @Test
    fun `tombstone with instance flow headers deletes the files, and a record without them is skipped`() {
        val fileIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        // fint-flyt-kafka 7 skipped a record it could not handle; the next one must still be processed.
        kafkaTemplate.send(ProducerRecord(instanceDeletedTopic.name, null)).get()
        kafkaTemplate
            .send(
                instanceDeletedTopic,
                InstanceFlowHeaders(sourceApplicationId = 1, correlationId = UUID.randomUUID(), fileIds = fileIds),
                null,
            ).get()

        verify(fileService, timeout(30_000)).delete(fileIds)
    }
}
