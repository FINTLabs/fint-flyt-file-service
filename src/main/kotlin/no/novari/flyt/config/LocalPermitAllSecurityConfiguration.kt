package no.novari.flyt.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@Profile("local-staging")
@ConditionalOnProperty(
    prefix = "novari.flyt.file-service.local-security",
    name = ["permit-all-enabled"],
    havingValue = "true",
)
class LocalPermitAllSecurityConfiguration {
    @Bean
    @Order(-100)
    fun localPermitAllSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/**")
            .csrf { csrf -> csrf.disable() }
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().permitAll()
            }

        return http.build()
    }
}
