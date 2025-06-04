package ui.clickupservice.sheet.web

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ui.clickupservice.shared.web.ApiResponse
import ui.clickupservice.sheet.service.UICashSheetService
import java.time.LocalDateTime

@RestController
@RequestMapping("/sheets")
class UISheetController(val uiCashSheetService: UICashSheetService) {

    @PostMapping("/syncPayments")
    fun sendPaymentReminder(): ApiResponse {

        uiCashSheetService.syncPlannedPayments().let {

            return ApiResponse("Payments synced: updated: ${it.first}, created: ${it.second}")
        }
    }

    @PostMapping("/updateCashPosition")
    fun updateCashPosition(): ApiResponse {
        uiCashSheetService.updateCashPosition()

        return ApiResponse("Cash position updated on ${LocalDateTime.now()}")
    }
}