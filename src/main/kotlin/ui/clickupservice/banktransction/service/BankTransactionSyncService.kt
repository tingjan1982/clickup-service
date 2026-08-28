package ui.clickupservice.banktransction.service

import org.springframework.stereotype.Service
import ui.clickupservice.bankexport.data.DebitBankTransaction
import ui.clickupservice.shared.TagConversionUtils
import ui.clickupservice.shared.extension.formatNumber
import ui.clickupservice.shared.extension.toDateFormat
import ui.clickupservice.shared.extension.toLocalDate
import ui.clickupservice.sheet.service.UICashSheetService
import ui.clickupservice.taskreminder.data.LoanTask
import ui.clickupservice.taskreminder.service.TaskService
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Service
class BankTransactionSyncService(val taskService: TaskService, val uiCashSheetService: UICashSheetService) {

    fun syncBankTransactions(transactions: List<DebitBankTransaction>): Int {

        val loanTasks = taskService.getLoanTasks()
            .filter { it.task.taskStatus != "paid" }
            .map { LoanCandidate.from(it) }

        val paymentTasks = taskService.getPlannedPaymentTasks()
            .filter { it.task.taskStatus != "paid" }
            .associateBy { "${TagConversionUtils.convertTag(it.task.toTagString())}-${it.task.dueDate.toDateFormat()}-${it.payment.formatNumber()}" }

        var taskCount = 0
        val matchedLoanTaskIds = mutableSetOf<String>()

        transactions.forEach { it ->
            val transactionAmount = it.amount
            val keyToSearch = "${it.entity}-${it.date.toDateFormat()}-${transactionAmount.formatNumber()}"

            paymentTasks[keyToSearch]?.let {
                val task = it.getWrappedTask()
                println("Found a payment task to update: ${task.id} - $keyToSearch")

                taskService.updateTaskStatus(task, "PAID")

                println(" Updated to PAID ${task.id}")
                taskCount++
            }

            val loanMatches = findLoanMatches(it, loanTasks.filterNot { candidate -> candidate.task.task.id in matchedLoanTaskIds })

            loanMatches.forEach { loanMatch ->
                val task = loanMatch.task.getWrappedTask()
                println("Found a loan task to update: ${task.id} - ${it.entity}-${it.date.toDateFormat()}-${transactionAmount.formatNumber()}")

                taskService.updateTaskStatus(task, "PAID")
                matchedLoanTaskIds.add(task.id)
                println(" Updated to PAID ${task.id}")
                taskCount++
            }
        }

        uiCashSheetService.syncPlannedPayments()
        return taskCount
    }

    private fun findLoanMatches(transaction: DebitBankTransaction, loanTasks: List<LoanCandidate>): List<LoanCandidate> {
        val sameEntityLoanTasks = loanTasks.filter { it.entity == transaction.entity }

        sameEntityLoanTasks
            .filter { it.payment.isEqualTo(transaction.amount) }
            .filter { transaction.date == it.dueDate || transaction.date == nextBusinessDay(it.dueDate) }
            .takeIf { it.size == 1 }
            ?.let { return it }

        return sameEntityLoanTasks
            .groupBy { it.paymentCycle }
            .filter { (paymentCycle, _) -> isMonthEndPaymentDate(transaction.date, paymentCycle) }
            .map { (_, cycleLoanTasks) -> cycleLoanTasks }
            .firstOrNull { cycleLoanTasks -> cycleLoanTasks.sumOf { it.payment }.isEqualTo(transaction.amount) }
            ?: emptyList()
    }

    private fun isMonthEndPaymentDate(transactionDate: LocalDate, paymentCycle: YearMonth): Boolean {
        val monthEnd = paymentCycle.atEndOfMonth()
        return transactionDate == monthEnd ||
            transactionDate == lastBusinessDayOfMonth(paymentCycle) ||
            transactionDate == nextBusinessDay(monthEnd)
    }

    private fun lastBusinessDayOfMonth(paymentCycle: YearMonth): LocalDate {
        var date = paymentCycle.atEndOfMonth()
        while (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            date = date.minusDays(1)
        }
        return date
    }

    private fun nextBusinessDay(date: LocalDate): LocalDate {
        var nextDate = date.plusDays(1)
        while (nextDate.dayOfWeek == DayOfWeek.SATURDAY || nextDate.dayOfWeek == DayOfWeek.SUNDAY) {
            nextDate = nextDate.plusDays(1)
        }
        return nextDate
    }

    private fun BigDecimal.isEqualTo(other: BigDecimal): Boolean {
        return compareTo(other) == 0
    }

    private data class LoanCandidate(
        val task: LoanTask,
        val entity: String,
        val dueDate: LocalDate,
        val payment: BigDecimal,
        val paymentCycle: YearMonth
    ) {
        companion object {
            fun from(task: LoanTask): LoanCandidate {
                val dueDate = task.task.dueDate.toLocalDate()
                return LoanCandidate(
                    task,
                    TagConversionUtils.convertTag(task.task.toTagString()),
                    dueDate,
                    task.payment,
                    YearMonth.from(dueDate)
                )
            }
        }
    }
}
