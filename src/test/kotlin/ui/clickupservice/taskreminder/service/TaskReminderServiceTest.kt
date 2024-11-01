package ui.clickupservice.taskreminder.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TaskReminderServiceTest(@Autowired val taskReminderService: TaskReminderService) {

    @Test
    fun checkTenantRentReview() {

        taskReminderService.checkTenantRentView()
    }

    @Test
    fun checkAndSendTenantOptionPeriod() {

        taskReminderService.checkAndSendTenantOptionPeriod()
    }

    @Test
    fun sendPaymentReminder() {

        taskReminderService.sendPaymentReminder()
    }

    @Test
    fun createPaymentSummaryContent() {

        println(taskReminderService.createPaymentSummaryContent())
    }
}