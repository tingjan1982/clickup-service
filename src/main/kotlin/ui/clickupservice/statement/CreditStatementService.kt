package ui.clickupservice.statement

import com.google.api.services.sheets.v4.model.ValueRange
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy
import org.springframework.stereotype.Service
import ui.clickupservice.shared.GoogleApiUtils
import ui.clickupservice.shared.TagConversionUtils
import ui.clickupservice.shared.exception.BusinessException
import ui.clickupservice.shared.extension.Extensions
import ui.clickupservice.shared.extension.Extensions.Companion.parseLocalDate
import ui.clickupservice.shared.extension.toDate
import ui.clickupservice.shared.extension.toDateFormat
import ui.clickupservice.taskreminder.data.CreateTask
import ui.clickupservice.taskreminder.data.PaymentTask
import ui.clickupservice.taskreminder.data.Tasks
import ui.clickupservice.taskreminder.service.TaskService
import ui.clickupservice.taskreminder.service.TaskService.Companion.PAYMENT_SCHEDULE_LIST_ID
import java.io.InputStream
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


@Service
class CreditStatementService(val googleApiUtils: GoogleApiUtils, val taskService: TaskService) {

    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yy", Locale.ENGLISH)
        const val COST_ANALYSIS_SHEET_ID = "1XLHMC8oM8jnfCtdh_KoFmd871lzaIBGOUVQATvzZFoE"

    }

    fun extractTransactions(statement: InputStream): String {

        PdfDocument(PdfReader(statement)).use { pdfDoc ->
            val pageCount = pdfDoc.numberOfPages

            var dueDate: LocalDate = LocalDate.now()
            var account = ""

            for (i in 1..pageCount) {
                val strategy = LocationTextExtractionStrategy()
                val text = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i), strategy)

                Regex("Due Date\\n(\\d{2} \\w{3} \\d{2})").find(text)?.let {
                    dueDate = LocalDate.parse(it.groupValues[1].trim(), formatter)
                    println("Due date: $dueDate")
                }

                Regex("Account Number\\n\\d{4} \\d{4} \\d{4} (\\d{4})").find(text)?.let {
                    account = it.groupValues[1]
                    println("Account: $account")
                }

                if (text.contains("Date of")) {
                    println("--- Page $i ---")

                    if (isStatementUploadedBefore(account, dueDate)) {
                        throw BusinessException("This statement [$account, $dueDate] has already been uploaded before")
                    }

                    val lines = postProcessingTransactionLines(text)

                    lines.forEach { line ->
                        println("<$line>")
                        Regex("^(\\d{2} \\w{3} \\d{2})(.+\\s+)(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+(?:\\.\\d+)?)( -)?").find(line)?.let {

                            val transaction = createTransaction(it, dueDate, account)
                            println(transaction)

                            writeTransaction(transaction)
                        }
                    }
                }
            }

            addUploadedStatementRecord(account, dueDate)

            return account
        }
    }

    private fun postProcessingTransactionLines(text: String): MutableList<String> {

        val lines = text.lines().toMutableList()
        var indexToRemove = 0

        for ((idx, line) in lines.withIndex()) {
            if (Regex("(^\\s+(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+(?:\\.\\d+)?)( -)?)").matches(line)) {
                println("Found straggler amount on a separate line, attempt merging with previous line: ${lines[idx - 1]}, $line")
                lines[idx - 1] = lines[idx - 1].trim('\n', '\r') + line
                println("Merged result: ${lines[idx - 1]}")
                indexToRemove = idx
            }
        }

        if (indexToRemove != 0) {
            lines.removeAt(indexToRemove)
        }

        return lines
    }

    fun isStatementUploadedBefore(account: String, dueDate: LocalDate): Boolean {

        val service = googleApiUtils.getSheetService()

        val response = service.spreadsheets().values()[COST_ANALYSIS_SHEET_ID, "Credit Card - Summary!ProcessedStatement"].execute()
        val values: List<List<Any>> = response.getValues()

        if (values.isEmpty()) {
            println("No data found.")
        } else {
            for (row in values) {
                println(row)

                if (row[0] == account && Extensions.parseLocalDate(row[1].toString()).isEqual(dueDate)) {
                    return true
                }
            }
        }

        return false
    }

    fun addUploadedStatementRecord(account: String, dueDate: LocalDate) {

        val service = googleApiUtils.getSheetService()

        val body = ValueRange().setValues(
            listOf(
                listOf(
                    account, dueDate.toDateFormat()
                )
            )
        )

        val result = service.spreadsheets().values().append(COST_ANALYSIS_SHEET_ID, "Credit Card - Summary!ProcessedStatement", body)
            .setValueInputOption("USER_ENTERED")
            .setInsertDataOption("INSERT_ROWS")
            .execute()

        println("Added statement record (${result.updates.updatedRows})")
    }

    fun writeTransaction(transaction: Transaction) {

        val service = googleApiUtils.getSheetService()

        val body = ValueRange().setValues(
            listOf(
                listOf(
                    transaction.account,
                    transaction.dueDate.toDateFormat(),
                    transaction.date.toDateFormat(),
                    transaction.description,
                    transaction.amount.toString(),
                    transaction.transactionType
                )
            )
        )

        val result = service.spreadsheets().values().append(COST_ANALYSIS_SHEET_ID, "Credit Card!A1", body)
            .setValueInputOption("USER_ENTERED")
            .setInsertDataOption("INSERT_ROWS")
            .execute()

        println("Created row (${result.updates.updatedRows})")
    }

    private fun createTransaction(it: MatchResult, dueDate: LocalDate, account: String): Transaction {

        return Transaction(
            date = LocalDate.parse(it.groups[1]?.value.toString(), formatter),
            description = it.groups[2]?.value.toString().trim(),
            amount = BigDecimal(it.groups[3]?.value.toString().replace(",", "")),
            transactionType = if (it.groups[4]?.value == null) Transaction.TransactionType.DEBIT else Transaction.TransactionType.CREDIT,
            dueDate = dueDate,
            account = account
        )
    }

    fun populateExpenseTasks(): Pair<String, String> {

        val service = googleApiUtils.getSheetService()

        val response = service.spreadsheets().values()[COST_ANALYSIS_SHEET_ID, "Credit Card - Summary!A1:G19"].execute()
        val values: List<List<Any>> = response.getValues()

        if (values.isEmpty()) {
            throw BusinessException("There is no expense found in Credit Card Summary sheet.")
        } else {
            values.iterator().let {
                val firstRow = it.next()
                val cardNumber = firstRow[1] as String
                val dueDate = firstRow[3] as String

                println("Card number: $cardNumber, dueDate: $dueDate")
                it.next()
                it.next()
                it.next()

                val parentTaskId = deleteCardSubTasks(cardNumber)

                it.forEach { row ->
                    val number = NumberFormat.getCurrencyInstance(Locale.US).parse(row[5] as String)
                    val amount = BigDecimal(number.toString())

                    if (amount.compareTo(BigDecimal.ZERO) != 0) {
                        println(row)

                        val entity = row[0] as String
                        val createdSubTask = createExpenseSubTask(parentTaskId, cardNumber, entity, dueDate, amount)
                        println("Created subtask - ${createdSubTask.name}")
                    }
                }

                return Pair(cardNumber, dueDate)
            }

        }
    }

    private fun deleteCardSubTasks(cardNumber: String): String {

        taskService.getCardTasks().first { it.task.name.contains(cardNumber) }
            .let {
                taskService.deleteSubTasks(it.task.id)

                return it.task.id
            }
    }

    private fun createExpenseSubTask(parentTaskId: String, cardNumber: String, entity: String, dueDate: String, amount: BigDecimal): Tasks.Task {

        val paymentCustomFieldId = "4f4cdee5-f89f-46fa-851f-a7dc45ae5543"
        val typeCustomFieldId = "39fe1993-01ad-4ad6-bbbe-5b63827097be"

        CreateTask(
            name = "$cardNumber - $entity expense",
            dueDate = parseLocalDate(dueDate).toDate(),
            parent = parentTaskId,
            tags = listOf(TagConversionUtils.reverseLookupTag(entity)),
            customFields = listOf(
                Tasks.Task.CustomField(id = paymentCustomFieldId, name = paymentCustomFieldId, value = amount.toString()),
                Tasks.Task.CustomField(id = typeCustomFieldId, name = typeCustomFieldId, value = PaymentTask.Type.EXPENSE.ordinal.toString())
            )
        ).let {
            return taskService.createTask(PAYMENT_SCHEDULE_LIST_ID, it)
        }
    }
}