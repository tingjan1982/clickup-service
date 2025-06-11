package ui.clickupservice.sheet.web

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import ui.clickupservice.bankexport.service.BankExportService
import ui.clickupservice.shared.extension.toDateFormat
import ui.clickupservice.shared.web.ApiResponse
import ui.clickupservice.sheet.service.UICashSheetService
import java.time.LocalDateTime

@RestController
@RequestMapping("/sheets")
class UISheetController(val uiCashSheetService: UICashSheetService, val bankExportService: BankExportService) {

    @PostMapping("/syncPayments")
    fun sendPaymentReminder(): ApiResponse {

        uiCashSheetService.syncPlannedPayments().let {

            return ApiResponse("Payments synced: updated: ${it.first}, created: ${it.second}")
        }
    }

    @PostMapping("/updateCashPosition")
    fun updateCashPosition(@RequestParam("file") file: MultipartFile): ApiResponse {

        bankExportService.readBankBalance(file.inputStream).let {
            uiCashSheetService.updateCashPosition(it)

            return ApiResponse("Cash position updated on ${LocalDateTime.now().toDateFormat()}")
        }
    }
}