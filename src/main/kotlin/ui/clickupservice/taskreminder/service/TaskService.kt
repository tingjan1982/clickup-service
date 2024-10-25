package ui.clickupservice.taskreminder.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import ui.clickupservice.shared.config.ConfigProperties
import ui.clickupservice.shared.exception.BusinessException
import ui.clickupservice.taskreminder.config.TaskConfigProperties
import ui.clickupservice.taskreminder.data.Tasks
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors

/**
 * Pretty print JSON reference: https://www.baeldung.com/java-json-pretty-print
 */
@Service
class TaskService(
    val objectMapper: ObjectMapper,
    val requestHelper: RequestHelper,
    val taskConfigProperties: TaskConfigProperties
) {

    companion object {
        const val PAYMENT_SCHEDULE_LIST_ID = "900303019042"
    }


    fun getUpcomingAndOverdueTasks(): List<Tasks.Task> {

        val params = HashMap<String, String>()
        params["archived"] = "false"
        params["order_by"] = "due_date"
        params["reverse"] = "true"
        val request = requestHelper.request("api/v2/list/$PAYMENT_SCHEDULE_LIST_ID/task", params)

        val httpClient = HttpClient.newBuilder().build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString()).let { it ->

            if (it.statusCode() != 200) {
                throw BusinessException("Problem getting tasks from ClickUp")
            }

            val tasks = objectMapper.readValue<Tasks>(it.body())
            val today = LocalDate.now()

            val overdueTasks = tasks.tasks.filter {
                val dueDate = LocalDate.ofInstant(it.dueDate.toInstant(), ZoneId.systemDefault())
                val days = ChronoUnit.DAYS.between(today, dueDate)

                return@filter days <= taskConfigProperties.upcomingTasksDays
            }

            return overdueTasks
        }

    }

    @Component
    class RequestHelper(private val config: ConfigProperties) {

        fun request(url: String, params: Map<String, String>): HttpRequest? {

            val query = params.keys.stream()
                .map { key: Any ->
                    "$key=" + URLEncoder.encode(
                        params[key],
                        StandardCharsets.UTF_8
                    )
                }
                .collect(Collectors.joining("&"))

            return HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("${config.endpoint}/$url?$query"))
                .header("Authorization", config.authenticationToken)
                .build()
        }
    }

}
