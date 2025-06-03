package ui.clickupservice.statement

import com.google.api.services.sheets.v4.model.ValueRange
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy
import org.springframework.stereotype.Service
import ui.clickupservice.shared.GoogleApiUtils
import ui.clickupservice.shared.extension.toDateFormat
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*



@Service
class CreditStatementService(val googleApiUtils: GoogleApiUtils) {

    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yy", Locale.ENGLISH)
        const val COST_ANALYSIS_SHEET_ID = "1XLHMC8oM8jnfCtdh_KoFmd871lzaIBGOUVQATvzZFoE"

    }

    fun extractTransaction(statement: File) {

        PdfDocument(PdfReader(statement)).use { pdfDoc ->
            val pageCount = pdfDoc.numberOfPages

            var dueDate: LocalDate = LocalDate.now()
            var account = ""

            for (i in 1..pageCount) {
                val strategy = LocationTextExtractionStrategy()
                val text = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i), strategy)

                Regex("Due Date\\n(\\d{2} \\w{3} \\d{2})").find(text)?.let {
                    dueDate = LocalDate.parse(it.groupValues[1].trim(), formatter)
                    println(dueDate)
                }

                Regex("Account Number\\n\\d{4} \\d{4} \\d{4} (\\d{4})").find(text)?.let {
                    account = it.groupValues[1]
                    println(account)
                }

                if (text.contains("Date of")) {
                    println("--- Page $i ---")
                    text.lines().forEach { line ->
                        Regex("^(\\d{2} \\w{3} \\d{2})(.+\\s+)(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+(?:\\.\\d+)?)( -)?").find(line)?.let {
                            println(line)

                            val transaction = createTransaction(it, dueDate, account)
                            println(transaction)

                            writeTransaction(transaction)
                        }
                    }
                }
            }
        }
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
}