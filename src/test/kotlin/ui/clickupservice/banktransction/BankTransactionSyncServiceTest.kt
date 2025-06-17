package ui.clickupservice.banktransction

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ui.clickupservice.bankexport.service.BankExportService
import ui.clickupservice.banktransction.service.BankTransactionSyncService
import ui.clickupservice.shared.FileProcessingUtils
import ui.clickupservice.shared.FileProcessingUtils.CSV_DIR
import java.io.File

@SpringBootTest
class BankTransactionSyncServiceTest(@Autowired val service: BankTransactionSyncService, @Autowired val bankExportService: BankExportService) {

    @Test
    fun syncBankTransactions() {

        FileProcessingUtils.getCsvFile(File(CSV_DIR, "transactions")).let { file ->
            bankExportService.readDebitTransactions(file.inputStream()).let {
                service.syncBankTransactions(it)
            }
        }

    }
}