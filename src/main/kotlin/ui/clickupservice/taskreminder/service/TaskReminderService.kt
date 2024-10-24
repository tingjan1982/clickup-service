package ui.clickupservice.taskreminder.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ui.clickupservice.emailservice.EmailService
import java.text.SimpleDateFormat

@Service
class TaskReminderService(val taskService: TaskService, val emailService: EmailService) {

    private val df = SimpleDateFormat("dd/MM/yyyy")

    /**
     * buildString usage: https://dev.to/pfilaretov42/nice-way-to-build-string-in-kotlin-nm4
     */
    @Scheduled(cron = "0 0 9 * * TUE")
    fun sendTaskReminder() {

        val tasksGroupedByStatus = taskService.getUpcomingAndOverdueTasks().groupBy {
            it.status1
        }

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

                it.value.forEach { t ->
                    val tags = t.tags.joinToString { it.name }

                    append(t.name.padEnd(45)).append("($tags)".padEnd(20)).append(" due on ${df.format(t.dueDate)}").appendLine()
                }

                appendLine()
            }
        }

        println(content)

        emailService.sendDynamicEmail("Upcoming Payments: ${tasksGroupedByStatus.flatMap { it.value }.size} in Total", content)
    }
}