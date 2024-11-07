package ui.clickupservice.shared.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import java.util.*


@Configuration
@EnableScheduling
@EnableWebSecurity
@EnableAspectJAutoProxy
class TaskServiceConfig {

    @Bean
    fun objectMapper(): ObjectMapper {

        val objectMapper = jacksonObjectMapper()
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        return objectMapper
    }

    /**
     * https://www.baeldung.com/java-spring-fix-403-error
     */
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {

        return http.csrf { it.disable() }
            .authorizeHttpRequests { auth ->
            auth.anyRequest().permitAll()
        }.build()
    }

    @PostConstruct
    fun postSetup() {
        println("Timezone has been set to ${TimeZone.getDefault()}")
    }
}