package ui.clickupservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.*


@SpringBootApplication
class ClickUpServiceApplication


fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("Australia/Brisbane"))

    runApplication<ClickUpServiceApplication>(*args)
}
