package ui.clickupservice.taskreminder.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "task")
class TaskConfigProperties {

    var upcomingTasksDays: Int = 0
}