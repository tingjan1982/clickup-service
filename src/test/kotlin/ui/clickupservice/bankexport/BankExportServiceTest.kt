package ui.clickupservice.bankexport

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ui.clickupservice.bankexport.service.BankExportService

@SpringBootTest
class BankExportServiceTest(@Autowired val service: BankExportService) {

    @Test
    fun readBankBalance() {

        service.readBankBalance().forEach {
            println(it)
        }
    }

    @Test
    fun readTransactions() {

        service.readDebitTransactions().forEach {
            println(it)
        }
    }
}