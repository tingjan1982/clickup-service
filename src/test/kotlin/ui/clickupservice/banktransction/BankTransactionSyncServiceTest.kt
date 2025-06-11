package ui.clickupservice.banktransction

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ui.clickupservice.bankexport.BankExportServiceTest.Companion.CSV_DIR
import ui.clickupservice.bankexport.BankExportServiceTest.Companion.getCsvFile
import ui.clickupservice.bankexport.service.BankExportService
import ui.clickupservice.banktransction.service.BankTransactionSyncService
import java.io.File

@SpringBootTest
class BankTransactionSyncServiceTest(@Autowired val service: BankTransactionSyncService, @Autowired val bankExportService: BankExportService) {

    @Test
    fun syncBankTransactions() {

        val csvFile = getCsvFile(File(CSV_DIR, "transactions"))

        bankExportService.readDebitTransactions(csvFile.inputStream()).let {
            service.syncBankTransactions(it)
        }
    }
}