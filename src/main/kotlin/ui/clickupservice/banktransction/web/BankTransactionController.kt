package ui.clickupservice.banktransction.web

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import ui.clickupservice.bankexport.service.BankExportService
import ui.clickupservice.banktransction.service.BankTransactionSyncService
import ui.clickupservice.shared.web.ApiResponse

@RestController
@RequestMapping("/transactions")
class BankTransactionController(val bankTransactionSyncService: BankTransactionSyncService, val bankExportService: BankExportService) {

    @PostMapping("/sync")
    fun synchronizeBankTransactions(@RequestParam("file") file: MultipartFile): ApiResponse {

        bankExportService.readDebitTransactions(file.inputStream).let { transactions ->
            bankTransactionSyncService.syncBankTransactions(transactions).let {
                return ApiResponse("Synced tasks: $it")
            }
        }
    }
}
