package ui.clickupservice.taskreminder.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ui.clickupservice.notion.data.Lease
import ui.clickupservice.shared.web.ApiResponse
import ui.clickupservice.taskreminder.service.ScheduledTaskService
import ui.clickupservice.taskreminder.service.TaskService
import ui.clickupservice.taskreminder.service.TenantService

@RestController
@RequestMapping("/services")
class TaskReminderController(
    val scheduledTaskService: ScheduledTaskService,
    val tenantService: TenantService,
    val taskService: TaskService
) {

    @GetMapping("/payments")
    fun sendPaymentReminder(): String {

        scheduledTaskService.sendPaymentReminder().let {
            return """
            <pre style="font-family: monospace">
$it
            </pre>""".trimIndent()
        }
    }

    @GetMapping("/rentReviewSummary")
    fun sendRentReviewSummary(): List<Lease> {

        return tenantService.sendRentReviewSummary()
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

    @PostMapping("/payments/completePaid")
    fun completePaidTasks(): ApiResponse {

        val completedTasks = taskService.completePaidTasks()
        val names = completedTasks.joinToString { it.name }

        return ApiResponse("Updated ${completedTasks.size} paid tasks to complete${if (names.isNotBlank()) ": $names" else ""}")
    }
}
