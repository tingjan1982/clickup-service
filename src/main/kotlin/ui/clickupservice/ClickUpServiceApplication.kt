package ui.clickupservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import ui.clickupservice.taskreminder.service.TaskService

@SpringBootApplication
class ClickUpServiceApplication


fun main(args: Array<String>) {
    runApplication<ClickUpServiceApplication>(*args)
}
