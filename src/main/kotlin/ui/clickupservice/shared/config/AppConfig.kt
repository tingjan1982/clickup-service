package ui.clickupservice.shared.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.*


@Configuration
@EnableScheduling
@EnableWebSecurity
@EnableAspectJAutoProxy
class TaskServiceConfig {

    @Bean
    fun objectMapper(): ObjectMapper {

        val objectMapper = jacksonObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        return objectMapper
    }

    /**
     * https://www.baeldung.com/java-spring-fix-403-error
     */
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {

        return http.csrf { it.disable() }
            .cors { }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.setAllowedOriginPatterns(
            listOf(
                "https://dashboard.rabybayharbour.online",
                "http://localhost:5173",
                "http://boardroom:5173"
            )
        )
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    @PostConstruct
    fun postSetup() {
        println("Timezone has been set to ${TimeZone.getDefault()}")
    }
}
