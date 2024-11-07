package ui.clickupservice.leasing.data

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.MapPropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory

/**
 * https://www.baeldung.com/spring-boot-json-properties
 * https://www.baeldung.com/kotlin/jackson-kotlin
 * https://www.baeldung.com/jackson-object-mapper-tutorial
 * https://www.baeldung.com/jackson-map *
 */
@Configuration
@ConfigurationProperties("leasing")
//@PropertySource(value = ["classpath:/config/application.json", "file:config/application.json"], ignoreResourceNotFound = true, factory = JsonPropertyFactory::class)
@RefreshScope
class LeasingConfigProperties {

    lateinit var cpiFigures: Map<String, Double>

    class JsonPropertyFactory : PropertySourceFactory {

        override fun createPropertySource(name: String?, resource: EncodedResource): org.springframework.core.env.PropertySource<*> {

            val typeRef: TypeReference<Map<String, Any>> = object : TypeReference<Map<String, Any>>() {}
            val readValue: Map<String, Any> = ObjectMapper().readValue(resource.inputStream, typeRef)

            return MapPropertySource("json-property", readValue)

        }
    }
}