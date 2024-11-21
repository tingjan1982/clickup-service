package ui.clickupservice.sheet.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ui.clickupservice.sheet.service.UICashSheetService

@RestController
@RequestMapping("/sheets")
class UISheetController(val uiCashSheetService: UICashSheetService) {

    @GetMapping("/syncPayments")
    fun sendPaymentReminder(): String {

        uiCashSheetService.syncPlannedPayments()
        return "synced"
    }
}