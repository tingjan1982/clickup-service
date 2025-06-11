package ui.clickupservice.sheet

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ui.clickupservice.sheet.service.UICashSheetService

@SpringBootTest
class UICashSheetServiceTest(@Autowired val uiCashSheetService: UICashSheetService) {


    @Test
    fun readCashPosition() {

        uiCashSheetService.readCashPosition()
    }

    @Test
    fun updateCashPosition() {

        //uiCashSheetService.updateCashPosition()
    }

    @Test
    fun syncPlannedPayments() {

        uiCashSheetService.syncPlannedPayments()
    }
}