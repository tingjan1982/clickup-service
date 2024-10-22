package ui.clickupservice.taskreminder.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import ui.clickupservice.shared.config.ConfigProperties
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


@Service
class TaskService(
    val objectMapper: ObjectMapper,
    val requestHelper: RequestHelper
) {

    fun getUpcomingAndOverdueTasks(): List<Tasks.Task> {

        val params = HashMap<String, String>()
        params["archived"] = "false"
        params["order_by"] = "due_date"
        params["reverse"] = "true"
        val listId = "900303019042"
        val request = requestHelper.request("api/v2/list/$listId/task", params)

        val httpClient = HttpClient.newBuilder().build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString()).let { it ->

            val tasks = objectMapper.readValue<Tasks>(it.body())
            val today = LocalDate.now()

            val overdueTasks = tasks.tasks.filter {
                val dueDate = LocalDate.ofInstant(it.dueDate.toInstant(), ZoneId.systemDefault())
                val days = ChronoUnit.DAYS.between(today, dueDate)

                if (days <= 7) {
                    println("${it.name} of ${it.tags} is due in $days days")
                }

                return@filter days <= 7
            }

            return overdueTasks
//            val prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tasks)
//            println(prettyJson)
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