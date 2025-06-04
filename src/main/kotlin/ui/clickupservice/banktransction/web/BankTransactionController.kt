package ui.clickupservice.banktransction.web

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ui.clickupservice.banktransction.service.BankTransactionSyncService
import ui.clickupservice.shared.web.ApiResponse

@RestController
@RequestMapping("/transactions")
class BankTransactionController(val bankTransactionSyncService: BankTransactionSyncService) {

    @PostMapping("/sync")
    fun synchronizeBankTransactions(): ApiResponse {

        bankTransactionSyncService.syncBankTransactions().let {
            return ApiResponse("Synced tasks: $it")
        }

    }
}