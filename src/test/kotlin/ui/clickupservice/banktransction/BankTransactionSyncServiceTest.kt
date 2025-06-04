package ui.clickupservice.banktransction

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ui.clickupservice.banktransction.service.BankTransactionSyncService

@SpringBootTest
class BankTransactionSyncServiceTest(@Autowired val service: BankTransactionSyncService) {

    @Test
    fun syncBankTransactions() {

        service.syncBankTransactions()
    }
}