package ui.clickupservice.taskreminder.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TaskReminderServiceTest(@Autowired val taskReminderService: TaskReminderService) {

    @Test
    fun sendTaskReminder() {

        taskReminderService.sendTaskReminder()
    }

    @Test
    fun createTaskSummaryContent() {

        println(taskReminderService.createTaskSummaryContent())
    }
}