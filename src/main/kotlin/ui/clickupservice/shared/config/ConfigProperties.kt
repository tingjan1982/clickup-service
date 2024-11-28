package ui.clickupservice.shared.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "config")
class ConfigProperties {

    lateinit var endpoint: String
    lateinit var authenticationToken: String
    lateinit var sendgridApiKey: String
    lateinit var notificationEmail: String
    lateinit var googleCredentials: String
}