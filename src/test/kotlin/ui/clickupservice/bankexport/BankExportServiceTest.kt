package ui.clickupservice.bankexport

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ui.clickupservice.bankexport.service.BankExportService
import ui.clickupservice.shared.FileProcessingUtils
import ui.clickupservice.shared.FileProcessingUtils.CSV_DIR
import java.io.File

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
}