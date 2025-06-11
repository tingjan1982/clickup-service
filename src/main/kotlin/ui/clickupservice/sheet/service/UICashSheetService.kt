package ui.clickupservice.sheet.service

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest
import com.google.api.services.sheets.v4.model.ValueRange
import org.springframework.stereotype.Service
import ui.clickupservice.bankexport.data.BankAccount
import ui.clickupservice.shared.GoogleApiUtils
import ui.clickupservice.shared.TagConversionUtils
import ui.clickupservice.shared.extension.formatNumber
import ui.clickupservice.shared.extension.toDateFormat
import ui.clickupservice.taskreminder.data.LoanTask
import ui.clickupservice.taskreminder.data.PaymentTask
import ui.clickupservice.taskreminder.service.TaskService
import java.time.LocalDate


/**
 * This is another way to load resource via @Value annotation.
 * @Value("file:./credentials.json") val resource: Resource,
 */
@Service
class UICashSheetService(
    val taskService: TaskService,
    val googleApiUtils: GoogleApiUtils
) {

    companion object {
        private const val SHEET_ID = "106RJju-J-NNvnu_TdfbxbZZUFUgIAp_Xheu3KKzE2dU"
        private const val TRANSACTIONS_RANGE = "Cashflow Planning!Transactions"
        private const val ROWS_BEFORE_TRANSACTIONS = 12
    }

    fun readCashPosition() {

        val range = "Cashflow Planning!A3:M6"
        val service = googleApiUtils.getSheetService()

        val response = service.spreadsheets().values()[SHEET_ID, range].execute()
        val values: List<List<Any>> = response.getValues()

        if (values.isEmpty()) {
            println("No data found.")
        } else {
            for (row in values) {
                println(row)
            }
        }
    }

    fun updateCashPosition(bankAccounts: Map<String, BankAccount>) {

        val service = googleApiUtils.getSheetService()

        val body = ValueRange()
            .setRange("Cashflow Planning!K1")
            .setValues(listOf(listOf(LocalDate.now().toString())))

        service.spreadsheets().values()
            .update(SHEET_ID, "Cashflow Planning!K1", body)
            .setValueInputOption("USER_ENTERED")
            .execute()

        val valueRange = ValueRange().setRange("Cashflow Planning!B4")
            .setValues(
                listOf(
                    listOf(
                        bankAccounts["BAB"]?.balance.toString(),
                        bankAccounts["CF"]?.balance.toString(),
                        bankAccounts["CF2"]?.balance.toString(),
                        bankAccounts["CF3"]?.balance.toString(),
                        bankAccounts["CF6"]?.balance.toString(),
                        bankAccounts["CF7"]?.balance.toString(),
                        bankAccounts["LPJP"]?.balance.toString(),
                        bankAccounts["PHKA"]?.balance.toString(),
                        bankAccounts["PHKD"]?.balance.toString(),
                        bankAccounts["RBM"]?.balance.toString(),
                        bankAccounts["RBHP"]?.balance.toString(),
                        bankAccounts["UI"]?.balance.toString()
                    )
                )
            )

        val request = BatchUpdateValuesRequest()
            .setValueInputOption("USER_ENTERED")
            .setData(listOf(valueRange))

        service.spreadsheets().values().batchUpdate(SHEET_ID, request).execute()
    }

    fun syncPlannedPayments(): Pair<Int, Int> {
        println("Sync planned payments...")

        val service = googleApiUtils.getSheetService()

        val data: MutableList<ValueRange> = mutableListOf()

        val paymentTasks = taskService.getPlannedPaymentTasks()
            .groupByTo(mutableMapOf()) {
                it.task.id
            }.also {
                println("Total planned payments: ${it.size}")
            }

        val loanTasks = taskService.getLoanTasks()
            .groupByTo(mutableMapOf()) {
                it.task.id
            }.also {
                println("Total planned loans: ${it.size}")

            }

        service.spreadsheets().values()[SHEET_ID, TRANSACTIONS_RANGE].execute().getValues().onEachIndexed { idx, row ->
            if (row[4] != "PAID") {
                syncPaymentTask(row, idx, paymentTasks, data)
                syncLoanTask(row, idx, loanTasks, data)
            }
        }

        val updatedCount = updateTasks(data, service)
        val createdCount = createTasks(paymentTasks, loanTasks, service)

        return Pair(updatedCount, createdCount)
    }

    private fun syncPaymentTask(row: MutableList<Any>, idx: Int, paymentTasks: MutableMap<String, MutableList<PaymentTask>>, data: MutableList<ValueRange>) {

        val paymentSk = if (row.size >= 7) {
            row[6]
        } else {
            ""
        }

        paymentTasks[paymentSk]?.let {
            val rowIdx = idx + ROWS_BEFORE_TRANSACTIONS
            val cellRange = "A${idx + ROWS_BEFORE_TRANSACTIONS}"
            println("Found payment ${row[0]} on row $rowIdx - $row")

            val task = it.first().task
            data.add(
                ValueRange().setRange(cellRange)
                    .setValues(
                        listOf(
                            listOf(
                                it.first().type.displayName,
                                task.dueDate.toDateFormat(),
                                it.first().payment.formatNumber(),
                                TagConversionUtils.convertTag(task.toTagString()),
                                task.taskStatus?.uppercase(),
                                task.name,
                                task.id,
                                "Last updated: ${LocalDate.now().toDateFormat()}"
                            )
                        )
                    )
            )

            paymentTasks.remove(paymentSk)
        }
    }

    private fun syncLoanTask(row: MutableList<Any>, idx: Int, loanTasks: MutableMap<String, MutableList<LoanTask>>, data: MutableList<ValueRange>) {

        val loanSk = if (row.size >= 7) {
            row[6]
        } else {
            ""
        }

        loanTasks[loanSk]?.let {
            val rowIdx = idx + ROWS_BEFORE_TRANSACTIONS
            val cellRange = "A${idx + ROWS_BEFORE_TRANSACTIONS}"
            println("Found loan ${row[0]} on row $rowIdx - ${row[6]}")

            val task = it.first().task
            data.add(
                ValueRange().setRange(cellRange)
                    .setValues(
                        listOf(
                            listOf(
                                PaymentTask.Type.INTEREST.displayName,
                                task.dueDate.toDateFormat(),
                                it.first().payment.formatNumber(),
                                TagConversionUtils.convertTag(task.toTagString()),
                                task.taskStatus?.uppercase(),
                                it.first().loan.formatNumber(),
                                task.id,
                                "Last updated: ${LocalDate.now().toDateFormat()}"
                            )
                        )
                    )
            )

            loanTasks.remove(loanSk)
        }
    }

    private fun updateTasks(data: MutableList<ValueRange>, service: Sheets): Int {

        print("Found ${data.size} records, updating..... ")
        val body = BatchUpdateValuesRequest()
            .setValueInputOption("USER_ENTERED")
            .setData(data)

        service.spreadsheets().values().batchUpdate(SHEET_ID, body).execute().let { result ->
            println("Total updated rows: ${result.totalUpdatedRows}")
            return result.totalUpdatedRows
        }
    }

    private fun createTasks(paymentTasks: MutableMap<String, MutableList<PaymentTask>>, loanTasks: MutableMap<String, MutableList<LoanTask>>, service: Sheets): Int {

        var taskCount = 0

        paymentTasks.filter {
            it.value.first().task.taskStatus !in arrayOf("payment plan", "paid")
        }.also {
            println("Total planned payments to create: ${it.size}")
        }.forEach {
            val task = it.value.first()
            val body = ValueRange().setValues(
                listOf(
                    listOf(
                        task.type.displayName,
                        task.task.dueDate.toDateFormat(),
                        task.payment,
                        TagConversionUtils.convertTag(task.task.toTagString()),
                        task.task.taskStatus?.uppercase(),
                        task.task.name,
                        task.task.id,
                        "Created: ${LocalDate.now().toDateFormat()}"
                    )
                )
            )

            val result = service.spreadsheets().values().append(SHEET_ID, TRANSACTIONS_RANGE, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute()

            println("Created row (${result.updates.updatedRows}): ${task.task.name}")
            taskCount += result.updates.updatedRows
        }

        loanTasks.filter {
            it.value.first().task.taskStatus !in arrayOf("payment plan", "paid")
        }.also {
            println("Total loans to create: ${it.size}")
        }.forEach {
            val task = it.value.first()
            val body = ValueRange().setValues(
                listOf(
                    listOf(
                        PaymentTask.Type.INTEREST.displayName,
                        task.task.dueDate.toDateFormat(),
                        task.payment,
                        TagConversionUtils.convertTag(task.task.toTagString()),
                        task.task.taskStatus?.uppercase(),
                        task.loan,
                        task.task.id,
                        "Created: ${LocalDate.now().toDateFormat()}"
                    )
                )
            )

            val result = service.spreadsheets().values().append(SHEET_ID, TRANSACTIONS_RANGE, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute()

            println("Created row (${result.updates.updatedRows}): ${task.task.name}")
            taskCount += result.updates.updatedRows
        }

        return taskCount
    }
}