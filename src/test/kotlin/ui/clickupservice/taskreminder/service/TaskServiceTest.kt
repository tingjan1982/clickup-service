package ui.clickupservice.taskreminder.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TaskServiceTest(@Autowired val service: TaskService) {

    @Test
    fun getTenancyScheduleTasks() {

        service.getTenancyScheduleTasks()
    }
}