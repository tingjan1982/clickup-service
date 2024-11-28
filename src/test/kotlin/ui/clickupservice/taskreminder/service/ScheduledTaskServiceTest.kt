package ui.clickupservice.taskreminder.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ScheduledTaskServiceTest(@Autowired val scheduledTaskService: ScheduledTaskService) {

    @Test
    fun sendTenantOptionPeriod() {

        scheduledTaskService.sendTenantOptionPeriod()
    }

    @Test
    fun sendPaymentReminder() {

        scheduledTaskService.sendPaymentReminder()
    }

    @Test
    fun createPaymentSummaryContent() {

        println(scheduledTaskService.createPaymentSummaryContent())
    }
}