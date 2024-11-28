package ui.clickupservice.taskreminder.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ui.clickupservice.taskreminder.service.ScheduledTaskService
import ui.clickupservice.taskreminder.service.TenantService

@RestController
@RequestMapping("/services")
class TaskReminderController(val scheduledTaskService: ScheduledTaskService, val tenantService: TenantService) {

    @GetMapping("/payments")
    fun sendPaymentReminder(): String {

        scheduledTaskService.sendPaymentReminder().let {
            return """
            <pre style="font-family: monospace">
$it
            </pre>""".trimIndent()
        }
    }

    @GetMapping("/optionPeriod")
    fun sendTenantOptionPeriod(): String {

        scheduledTaskService.sendTenantOptionPeriod().let {
            return """
            <pre style="font-family: monospace">
$it
            </pre>""".trimIndent()
        }
    }

    @GetMapping("/rentReview")
    fun sendTenantsInRentReview(): String {

        tenantService.sendTenantsInRentReview().let {
            return """
            <pre style="font-family: monospace">
$it
            </pre>""".trimIndent()
        }
    }
}