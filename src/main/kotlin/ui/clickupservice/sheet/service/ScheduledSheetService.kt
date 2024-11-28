package ui.clickupservice.sheet.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ScheduledSheetService(val uiCashSheetService: UICashSheetService) {

    @Scheduled(cron = "0 0 12 * * *")
    fun scheduleSyncPlannedPayments() {

        uiCashSheetService.syncPlannedPayments()
    }
}