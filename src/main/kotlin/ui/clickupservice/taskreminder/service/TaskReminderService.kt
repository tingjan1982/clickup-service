package ui.clickupservice.taskreminder.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ui.clickupservice.emailservice.EmailService
import ui.clickupservice.leasing.service.LeasingService
import ui.clickupservice.shared.extension.formatNumber
import ui.clickupservice.shared.extension.toDateFormat
import ui.clickupservice.shared.extension.toLocalDate
import ui.clickupservice.taskreminder.data.PaymentTask
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class TaskReminderService(val taskService: TaskService, val leasingService: LeasingService, val emailService: EmailService) {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(TaskReminderService::class.java)
    }

    fun checkTenantRentView() {

        val tenancies = taskService.getTenancyScheduleTasks().filter {
            val now = LocalDate.now().month
            val t = it.task

            if (t.taskStatus == "lease commenced") {
                val diff = now.compareTo(t.dueDate.toLocalDate().month)
                return@filter diff == -1
            }

            return@filter t.taskStatus == "rent review"
        }.onEach { it ->
            val t = it.task

            if (t.taskStatus != "rent review") {
                println("Updating task ${t.name} to RENT REVIEW")

                taskService.updateTaskStatus(t, "rent review").let {
                    println("Task has successfully been updated - ${it.taskStatus}")
                }
            }
        }

        val content = buildString {
            appendLine("Tenants Upcoming Rent Review")
            appendLine()
            append("${"Tenancy".padEnd(30)} | ${"Due Date".padEnd(12)} | ${"Current Rent".padEnd(12)} | New Rent").appendLine()
            appendLine("".padEnd(75, '-'))

            tenancies.forEach { t ->
                val it = t.task
                val cpiRent = try {
                    val cpiRent = leasingService.calculateRent(t.rent, t.task.dueDate)
                    "${cpiRent.first.formatNumber()} (${cpiRent.second})"

                } catch (_: Exception) {
                    "Waiting for CPI"
                }

                append(it.name.padEnd(30)).append(" | ").append(it.dueDate.toDateFormat().padEnd(12)).append(" | ")
                    .append(t.rent.formatNumber().padEnd(12)).append(" | ").append(cpiRent).appendLine()
            }
        }
        println(content)

    }

    @Scheduled(cron = "0 0 9 1W * TUE")
    fun checkAndSendTenantOptionPeriod(): String {

        val today = LocalDate.now()
        val tasks = taskService.getTenancyScheduleTasks().filter {
            val t = it.task

            if (t.taskStatus == "lease commenced") {
                val months = ChronoUnit.MONTHS.between(today, t.dueDate.toLocalDate())

                return@filter months <= 6
            }

            return@filter t.taskStatus == "option period"
        }.onEach { it ->
            val t = it.task
            if (t.taskStatus != "option period") {
                println("Updating task ${t.name} to OPTION PERIOD")

                taskService.updateTaskStatus(t, "option period").let {
                    println("Task has successfully been updated - ${it.taskStatus}")
                }
            }
        }

        val content = buildString {
            appendLine("Tenants in Option Period")
            appendLine()
            append("${"Tenancy".padEnd(30)} | ${"Due Date".padEnd(12)} | Entity").appendLine()
            appendLine("".padEnd(70, '-'))

            tasks.forEach {
                val t = it.task
                append(t.name.padEnd(30)).append(" | ").append(t.dueDate.toDateFormat().padEnd(12)).append(" | ").append(t.toTagString())
                    .appendLine()
            }

            appendLine()
        }

        println(content)
        emailService.sendDynamicEmail("Tenants in Option Period", content, mapOf("listId" to TaskService.TENANCY_SCHEDULE_LIST_ID))
        return content
    }

    /**
     * buildString usage: https://dev.to/pfilaretov42/nice-way-to-build-string-in-kotlin-nm4
     */
    @Scheduled(cron = "0 0 9 * * TUE")
    fun sendPaymentReminder(): String {

        LOGGER.info("Sending task reminder at ${Date()}")

        val tasksGroupedByStatus = getScheduledOrTodoTasks()

        val content = createPaymentSummaryContent(tasksGroupedByStatus)
        println(content)

        emailService.sendDynamicEmail(
            "Upcoming Payments: ${tasksGroupedByStatus.flatMap { it.value }.size} in Total",
            content,
            mapOf("listId" to TaskService.PAYMENT_SCHEDULE_LIST_ID)
        )
        return content
    }

    private fun getScheduledOrTodoTasks() = taskService.getUpcomingAndOverdueTasks()
        .filter {
            it.taskStatus != "payment plan"
        }.map { it ->
            val paymentField = it.customFields.first { it.name == "Payment" }
            return@map PaymentTask(it, paymentField.value ?: BigDecimal.ZERO)

        }
        .groupBy {
            it.task.taskStatus
        }

    fun createPaymentSummaryContent(tasksGroupedByStatus: Map<String?, List<PaymentTask>> = getScheduledOrTodoTasks()): String {
        val content = buildString {
            var paddingLength: Int

            appendLine("Upcoming Payments Summary")
            appendLine()
            appendLine("Please see the following upcoming payments grouped by status:")
            appendLine()

            tasksGroupedByStatus.forEach { it ->
                appendLine("Tasks in ${it.key?.uppercase()}".let {
                    paddingLength = it.length
                    return@let it
                })

                appendLine("".padEnd(paddingLength, '='))

                it.value.forEach { tt ->
                    val t = tt.task

                    append(t.name.padEnd(45)).append("(${t.toTagString()})".padEnd(20))
                        .append(" due on ${t.dueDate.toDateFormat()}".padEnd(10))
                        .append("  $${tt.payment}")
                        .appendLine()
                }

                appendLine()
            }
        }

        return content
    }

}