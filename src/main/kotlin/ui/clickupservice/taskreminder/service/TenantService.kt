package ui.clickupservice.taskreminder.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import ui.clickupservice.emailservice.EmailService
import ui.clickupservice.leasing.service.LeasingService
import ui.clickupservice.notion.data.Lease
import ui.clickupservice.shared.extension.*
import ui.clickupservice.taskreminder.data.TenantTask
import java.math.BigDecimal
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class TenantService(val taskService: TaskService, val leasingService: LeasingService, val emailService: EmailService) {

    fun getRentReviewSummary(): List<Lease> {

        return leasingService.getLeases()
            .filter { it.rentReviewDate.isBefore(LocalDate.now()) }
            .map {
                it.rentReviews.findLast { r -> r.year == LocalDate.now().year }?.let { rr ->
                    rr.awaitCPI = it.reviewType == "CPI" && rr.adoptedCPI <= BigDecimal.ZERO
                    it.rentReviews.clear()
                    it.rentReviews.add(rr)
                }

                return@map it
            }
    }

    fun sendRentReviewSummary(email: String) {

        getRentReviewSummary().let {
            val emailContent = buildString {
                appendLine("Tenant(s) that requires rent review.")
                appendLine()
                it.forEach { lease ->
                    appendLine("${lease.tenant} - anniversary on ${lease.rentReviewDate.toDateFormat()} Review Type (${lease.reviewType})")
                    appendLine()
                    append("New Rent: ")

                    lease.rentReviews.forEach { r ->
                        val newRent = if (r.awaitCPI) {
                            "Await CPI"
                        } else {
                            "${r.newRent.formatNumber()}+GST"
                        }

                        appendLine(newRent)
                    }

                    appendLine()
                }
            }

            emailService.sendBrevoEmail("Rent Review Summary - ${LocalDate.now().year}", emailContent, email)
        }
    }

    fun sendTenantsInRentReview(): String {

        val now = LocalDate.now()
        val currentQuarter = now.getQuarter()

        val tenancies = taskService.getTenancyScheduleTasks().filter {
            val t = it.task

            if (t.taskStatus == "lease commenced" && it.reviewType.needReview) {
                return@filter it.anniversaryDate.isInReviewPeriod(currentQuarter)
            }

            return@filter t.taskStatus in arrayOf("rent review", "rent reviewed")
        }.onEach {
            val t = it.task

            if (t.taskStatus == "lease commenced") {
                taskService.updateTaskStatus(t, "rent review")
            }
        }

        val content = buildString {
            appendLine("Upcoming Tenants Rent Review (All figures are GST exclusive)")
            appendLine()
            append("${"Tenancy".padEnd(25)} | ${"Anniversary".padEnd(12)} | ${"Current Rent".padEnd(12)} | ${"New Rent".padEnd(10)} | New Monthly Rent").appendLine()
            appendLine("".padEnd(100, '-'))

            val cpiFigure = try {
                leasingService.getCpiFigure(now).toString()
            } catch (_: Exception) {
                "Waiting for CPI"
            }

            tenancies.forEach { t ->
                val it = t.task
                var newRent = t.newRent
                val reviewType = t.reviewType

                if (newRent == BigDecimal.ZERO) {
                    newRent = try {
                        logger.info { "${it.name} - reviewing anniversary rent" }

                        when (t.reviewType) {
                            TenantTask.ReviewType.CPI -> leasingService.calculateCpiRent(t.rent, t.anniversaryDate).let { cpiRent ->
                                taskService.updateCustomField(it, "New Rent", cpiRent.formatNumber())
                                taskService.updateTaskStatus(it, "rent reviewed")

                                return@let cpiRent
                            }

                            TenantTask.ReviewType.PERCENT -> leasingService.calculatePercentageRent(t.rent, t.percentage).let { fixedRent ->
                                taskService.updateCustomField(it, "New Rent", fixedRent.formatNumber())
                                taskService.updateTaskStatus(it, "rent reviewed")

                                return@let fixedRent
                            }

                            else -> throw Exception("Shouldn't get here")
                        }

                    } catch (e: Exception) {
                        logger.error(e.message, e)
                        BigDecimal.ZERO
                    }
                }

                val monthlyRent = if (newRent > BigDecimal.ZERO) (newRent / BigDecimal(12) - t.monthlyIncentive).formatNumber() else ""
                append("${it.name} (${reviewType})".padEnd(25)).append(" | ").append(t.anniversaryDate.toDateFormat().padEnd(12))
                    .append(" | ")
                    .append(t.rent.formatNumber().padEnd(12)).append(" | ")
                    .append(newRent.formatNumber().padEnd(10)).append((" | "))
                    .append(monthlyRent)

                if (monthlyRent.isNotBlank()) {
                    append(" (${newRent.formatNumber()} / 12 - ${t.monthlyIncentive.formatNumber()})")
                }

                appendLine()
            }

            appendLine().appendLine("Q${now.getLastQuarter()} CPI used: $cpiFigure")
        }
        println(content)
        emailService.sendDynamicEmail("Tenant Rent Review", content, mapOf("listId" to TaskService.TENANCY_SCHEDULE_LIST_ID))
        return content
    }
}
