package no.novari

import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationRequestService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.listener.AbstractMessageListenerContainer
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(
    classes = [Application::class, ApplicationContextTest.KafkaListenerTestConfiguration::class],
    properties = [
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.listener.auto-startup=false",
    ],
)
@ActiveProfiles("local-staging")
@MockitoBean(types = [SourceApplicationAuthorizationRequestService::class])
class ApplicationContextTest {
    @Test
    fun contextLoads() {
    }

    @TestConfiguration(proxyBeanMethods = false)
    class KafkaListenerTestConfiguration {
        @Bean
        fun kafkaListenerAutoStartupDisabler(): BeanPostProcessor {
            return object : BeanPostProcessor {
                override fun postProcessBeforeInitialization(
                    bean: Any,
                    beanName: String,
                ): Any {
                    if (bean is AbstractMessageListenerContainer<*, *>) {
                        bean.setAutoStartup(false)
                    }

                    return bean
                }
            }
        }
    }
}
