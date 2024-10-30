package ui.clickupservice.taskreminder.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ui.clickupservice.emailservice.EmailService
import ui.clickupservice.taskreminder.data.TransformedTask
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@Service
class TaskReminderService(val taskService: TaskService, val emailService: EmailService) {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(TaskReminderService::class.java)
        val df = SimpleDateFormat("dd/MM/yyyy")
    }

    /**
     * buildString usage: https://dev.to/pfilaretov42/nice-way-to-build-string-in-kotlin-nm4
     */
    @Scheduled(cron = "0 0 9 * * TUE")
    fun sendTaskReminder(): String {

        LOGGER.info("Sending task reminder at ${Date()}")

        val tasksGroupedByStatus = getScheduledOrTodoTasks()

        val content = createTaskSummaryContent(tasksGroupedByStatus)
        println(content)

        emailService.sendDynamicEmail(
            "Upcoming Payments: ${tasksGroupedByStatus.flatMap { it.value }.size} in Total",
            content
        )
        return content
    }


    private fun getScheduledOrTodoTasks() = taskService.getUpcomingAndOverdueTasks()
        .filter {
            it.taskStatus != "payment plan"
        }.map {
            val paymentField = it.customFields.filter { it.name == "Payment" }.first()
            return@map TransformedTask(it, paymentField.value ?: BigDecimal.ZERO)

        }
        .groupBy {
            it.task.taskStatus
        }

    fun createTaskSummaryContent(tasksGroupedByStatus: Map<String?, List<TransformedTask>> = getScheduledOrTodoTasks()): String {
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
                    val tags = t.tags.joinToString { it.name }

                    append(t.name.padEnd(45)).append("($tags)".padEnd(20))
                        .append(" due on ${df.format(t.dueDate)}".padEnd(10))
                        .append("  $${tt.payment}")
                        .appendLine()
                }

                appendLine()
            }
        }

        return content
    }
}