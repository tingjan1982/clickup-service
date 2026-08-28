package ui.clickupservice.bankexport

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ui.clickupservice.bankexport.service.BankExportService
import ui.clickupservice.shared.FileProcessingUtils
import ui.clickupservice.shared.FileProcessingUtils.CSV_DIR
import java.io.File
import java.math.BigDecimal
import kotlin.test.assertEquals

@SpringBootTest
class BankExportServiceTest(@Autowired val service: BankExportService) {

    @Test
    fun readBankBalance() {

        FileProcessingUtils.getCsvFile(File(CSV_DIR)).let { file ->
            service.readBankBalance(file.inputStream()).forEach {
                println(it)
            }

            FileProcessingUtils.markFileAsProcessed(file)
        }
    }

    @Test
    fun readTransactions() {

        FileProcessingUtils.getCsvFile(File("$CSV_DIR/transactions")).let { file ->
            service.readDebitTransactions(file.inputStream()).forEach {
                println(it)
            }

            FileProcessingUtils.markFileAsProcessed(file)
        }
    }

    @Test
    fun readTransactionsIncludesCreditTransactions() {

        val csv = """
            Account Number,Date,Narrative,Debit Amount,Credit Amount
            000000508462,01/01/2026,Debit payment,123.45,
            000000159960,02/01/2026,Credit payment,,456.78
            000000537677,03/01/2026,Zero row,,
        """.trimIndent()

        val transactions = service.readDebitTransactions(csv.byteInputStream())

        assertEquals(3, transactions.size)
        assertEquals(BigDecimal("123.45"), transactions[0].amount)
        assertEquals(BigDecimal.ZERO, transactions[0].creditAmount)
        assertEquals(BigDecimal("-456.78"), transactions[1].amount)
        assertEquals(BigDecimal.ZERO, transactions[1].debitAmount)
    }
}
