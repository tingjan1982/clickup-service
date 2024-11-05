package ui.clickupservice.taskreminder.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ui.clickupservice.taskreminder.service.TaskReminderService
import ui.clickupservice.taskreminder.service.TenantService

@RestController
@RequestMapping("/services")
class TaskReminderController(val taskReminderService: TaskReminderService, val tenantService: TenantService) {

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

        taskReminderService.sendTenantOptionPeriod().let {
            return """
            <pre style="font-family: monospace">
$it
            </pre>""".trimIndent()
        }
    }

    @GetMapping("/rentReview")
    fun sendTenantRentReviewReminder(): String {

        tenantService.sendTenantRentReview().let {
            return """
            <pre style="font-family: monospace">
$it
            </pre>""".trimIndent()
        }
    }
}