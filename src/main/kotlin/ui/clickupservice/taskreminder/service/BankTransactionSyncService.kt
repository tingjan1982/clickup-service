package ui.clickupservice.taskreminder.service

import org.springframework.stereotype.Service
import ui.clickupservice.bankexport.service.BankExportService
import ui.clickupservice.shared.TagConversionUtils
import ui.clickupservice.shared.extension.toDateFormat
import ui.clickupservice.sheet.service.UICashSheetService

@Service
class BankTransactionSyncService(val bankExportService: BankExportService, val taskService: TaskService, val uiCashSheetService: UICashSheetService) {

    fun syncBankTransactionsWithTasks() {

        val loanTasks = taskService.getLoanTasks()
            .filter { it.task.taskStatus != "paid" }
            .associateBy { "${TagConversionUtils.convertTag(it.task.toTagString())}-${it.payment}" }

        val paymentTasks = taskService.getPlannedPaymentTasks()
            .filter { it.task.taskStatus != "paid" }
            .associateBy { "${TagConversionUtils.convertTag(it.task.toTagString())}-${it.task.dueDate.toDateFormat()}-${it.payment}" }

        bankExportService.readDebitTransactions().forEach { it ->
            val keyToSearch = "${it.entity}-${it.date.toDateFormat()}-${it.debitAmount.stripTrailingZeros()}"

            paymentTasks[keyToSearch]?.let {
                val task = it.getWrappedTask()
                println("Found a payment task to update: ${task.id} - $keyToSearch")

                taskService.updateTaskStatus(task, "PAID")

                println(" Updated to PAID ${task.id}")
            }

            val loanKeyToSearch = "${it.entity}-${it.debitAmount.stripTrailingZeros()}"

            loanTasks[loanKeyToSearch]?.let {
                val task = it.getWrappedTask()
                println("Found a loan task to update: ${task.id} - $loanKeyToSearch")

                taskService.updateTaskStatus(task, "PAID")

                println(" Updated to PAID ${task.id}")
            }

        }

        uiCashSheetService.syncPlannedPayments()
    }
}