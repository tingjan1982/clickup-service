package ui.clickupservice.taskreminder.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ui.clickupservice.taskreminder.service.TaskReminderService

@RestController
@RequestMapping("/task-reminders")
class TaskReminderController(val taskReminderService: TaskReminderService) {

    @GetMapping("/payments")
    fun sendPaymentReminder(): String {

        taskReminderService.sendPaymentReminder().let {
            return """
            <pre style="font-family: monospace">
$it
            </pre>""".trimIndent()
        }
    }

    @GetMapping("/optionPeriod")
    fun sendOptionPeriodReminder(): String {

        taskReminderService.checkAndSendTenantOptionPeriod().let {
            return """
            <pre style="font-family: monospace">
$it
            </pre>""".trimIndent()
        }
    }

}