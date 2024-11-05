package ui.clickupservice.taskreminder.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ui.clickupservice.emailservice.EmailService
import ui.clickupservice.leasing.service.LeasingService
import ui.clickupservice.shared.extension.*
import java.math.BigDecimal
import java.time.LocalDate

@Service
class TenantService(val taskService: TaskService, val leasingService: LeasingService, val emailService: EmailService) {

    @Scheduled(cron = "0 0 0 L * *")
    fun updateTenantsInRentReview() {
        val lastQuarter = LocalDate.now().getLastQuarter()

        taskService.getTenancyScheduleTasks().filter {
            return@filter it.task.taskStatus == "rent reviewed"
        }.forEach {
            val isLastQuarter = it.anniversaryDate.getQuarter() == lastQuarter

            if (isLastQuarter) {
                print("${it.task.name} - updating status to Lease Commenced")
                taskService.updateTaskStatus(it.task, "lease commenced").also {
                    print(" - Success")
                }
                println()
                taskService.updateCustomField(it.task, "Annual Rent", it.newRent.formatNumber())
                taskService.updateCustomField(it.task, "New Rent", "")
            }
        }
    }

    @Scheduled(cron = "0 0 8 1 * *")
    fun sendTenantRentReview(): String {

        val tenancies = taskService.getTenancyScheduleTasks().filter { it ->
            val t = it.task

            if (t.taskStatus == "lease commenced" && it.reviewType.needReview) {
                return@filter it.anniversaryDate.isInReviewPeriod()
            }

            return@filter t.taskStatus in arrayOf("rent review", "rent reviewed")
        }.onEach { it ->
            val t = it.task

            if (t.taskStatus == "lease commenced") {
                print("${t.name} - updating status to Rent Review")

                taskService.updateTaskStatus(t, "rent review").also {
                    print(" - Success")
                }

                println()
            }
        }

        val content = buildString {
            appendLine("Upcoming Tenants Rent Review (All figures are GST exclusive)")
            appendLine()
            append("${"Tenancy".padEnd(25)} | ${"Anniversary".padEnd(12)} | ${"Current Rent".padEnd(12)} | ${"New Rent".padEnd(12)} | ${"CPI".padEnd(10)} | New Monthly Rent").appendLine()
            appendLine("".padEnd(100, '-'))

            tenancies.forEach { t ->
                val it = t.task
                var newRent = t.newRent
                var cpiFigure = ""

                if (newRent == BigDecimal.ZERO) {

                    newRent = try {
                        println("${it.name} - calculating CPI rent")

                        leasingService.calculateRent(t.rent, t.anniversaryDate).let { (cpiRent, cpi) ->
                            cpiFigure = cpi.toString()
                            taskService.updateCustomField(it, "New Rent", cpiRent.formatNumber())
                            taskService.updateTaskStatus(it, "rent reviewed")

                            return@let cpiRent
                        }

                    } catch (e: Exception) {
                        println(e)
                        BigDecimal.ZERO
                    }
                }

                val monthlyRent = if (newRent > BigDecimal.ZERO) (newRent / BigDecimal(12) - t.monthlyIncentive).formatNumber() else ""

                append(it.name.padEnd(25)).append(" | ").append(t.anniversaryDate.toDateFormat().padEnd(12)).append(" | ")
                    .append(t.rent.formatNumber().padEnd(12)).append(" | ")
                    .append(newRent.formatNumber().padEnd(12)).append((" | "))
                    .append(cpiFigure.padEnd(10)).append((" | "))
                    .append(monthlyRent.padEnd(10))

                if (monthlyRent.isNotBlank()) {
                    append("breakdown: (${newRent.formatNumber()} / 12 - ${t.monthlyIncentive.formatNumber()})")
                }

                appendLine()
            }
        }
        println(content)
        emailService.sendDynamicEmail("Quarterly Tenant Rent Review", content, mapOf("listId" to TaskService.TENANCY_SCHEDULE_LIST_ID))
        return content
    }
}