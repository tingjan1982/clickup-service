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

    @Test
    fun getPlannedPaymentTasks() {

        service.getPlannedPaymentTasks().forEach { t ->
            val it = t.task
            println("${it.name} ${it.id} ${it.toTagString()}")
        }
    }

    @Test
    fun getLoanTasks() {

        service.getLoanTasks().forEach { t ->
            val it = t.task
            println("${it.name} ${it.id} ${it.toTagString()}")
        }
    }

    @Test
    fun getCardTasks() {

        service.getCardTasks().forEach { t ->
            println(t)
        }
    }


    @Test
    fun getAndDeleteCard1212Subtasks() {

        service.getTaskById("86czj3x4c").let { it ->
            println(it)

            service.deleteSubTasks(it.id)
        }
    }
}