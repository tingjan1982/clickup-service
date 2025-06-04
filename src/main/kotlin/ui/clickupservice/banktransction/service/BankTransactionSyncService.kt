package ui.clickupservice.banktransction.service

import org.springframework.stereotype.Service
import ui.clickupservice.bankexport.service.BankExportService
import ui.clickupservice.shared.TagConversionUtils
import ui.clickupservice.shared.extension.formatNumber
import ui.clickupservice.shared.extension.toDateFormat
import ui.clickupservice.sheet.service.UICashSheetService
import ui.clickupservice.taskreminder.service.TaskService

@Service
class BankTransactionSyncService(val bankExportService: BankExportService, val taskService: TaskService, val uiCashSheetService: UICashSheetService) {

    fun syncBankTransactions(): Int {

        val loanTasks = taskService.getLoanTasks()
            .filter { it.task.taskStatus != "paid" }
            .associateBy { "${TagConversionUtils.convertTag(it.task.toTagString())}-${it.payment.formatNumber()}" }

        val paymentTasks = taskService.getPlannedPaymentTasks()
            .filter { it.task.taskStatus != "paid" }
            .associateBy { "${TagConversionUtils.convertTag(it.task.toTagString())}-${it.task.dueDate.toDateFormat()}-${it.payment.formatNumber()}" }

        var taskCount = 0
        bankExportService.readDebitTransactions().forEach { it ->
            val keyToSearch = "${it.entity}-${it.date.toDateFormat()}-${it.debitAmount.formatNumber()}"

            paymentTasks[keyToSearch]?.let {
                val task = it.getWrappedTask()
                println("Found a payment task to update: ${task.id} - $keyToSearch")

                taskService.updateTaskStatus(task, "PAID")

                println(" Updated to PAID ${task.id}")
                taskCount++
            }

            val loanKeyToSearch = "${it.entity}-${it.debitAmount.formatNumber()}"

            loanTasks[loanKeyToSearch]?.let {
                val task = it.getWrappedTask()
                println("Found a loan task to update: ${task.id} - $loanKeyToSearch")

                taskService.updateTaskStatus(task, "PAID")

                println(" Updated to PAID ${task.id}")
                taskCount++
            }

        }

        uiCashSheetService.syncPlannedPayments()
        return taskCount
    }
}