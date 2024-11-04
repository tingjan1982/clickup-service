package ui.clickupservice.taskreminder.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import ui.clickupservice.shared.config.ConfigProperties
import ui.clickupservice.shared.exception.BusinessException
import ui.clickupservice.shared.extension.toLocalDate
import ui.clickupservice.taskreminder.config.TaskConfigProperties
import ui.clickupservice.taskreminder.data.Tasks
import ui.clickupservice.taskreminder.data.TenantTask
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.LocalDate
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
        const val TENANCY_SCHEDULE_LIST_ID = "900302094609"
    }

    fun getTenancyScheduleTasks(): List<TenantTask> {
        val params = HashMap<String, String>()
        params["archived"] = "false"
        val now = LocalDate.now()

        return getTaskRequest(TENANCY_SCHEDULE_LIST_ID, params).tasks.map { it ->
            val rentField = it.customFields.first { it.name == "Annual Rent" }.toBigDecimal()
            val newRentField = it.customFields.first { it.name == "New Rent" }.toBigDecimal()
            val monthlyIncentiveField = it.customFields.first { it.name == "Monthly Incentive" }.toBigDecimal()
            val reviewType = it.customFields.first { it.name == "Review Type" }.toEnumType<TenantTask.ReviewType>(TenantTask.ReviewType.NA)

            val anniversary = it.startDate.toLocalDate().let {
                val year = if (it.month < now.month) now.year + 1 else now.year

                return@let LocalDate.of(year, it.month, it.dayOfMonth)
            }
            return@map TenantTask(it, rentField, newRentField, monthlyIncentiveField, reviewType, anniversary)
        }
    }

    fun updateTaskStatus(task: Tasks.Task, status: String): Tasks.Task {

        val payload = mapOf("status" to status)
        val request = requestHelper.putRequest("api/v2/task/${task.id}", payload)
        val httpClient = HttpClient.newBuilder().build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString()).let { it ->

            if (it.statusCode() != 200) {
                throw BusinessException("Problem updating tasks in ClickUp")
            }

            return objectMapper.readValue<Tasks.Task>(it.body())
        }
    }

    fun updateCustomField(task: Tasks.Task, fieldName: String, value: String): String {

        val fieldId = task.customFields.first { it.name == fieldName }.id
        val payload = mapOf("value" to value)
        val request = requestHelper.postRequest("api/v2/task/${task.id}/field/${fieldId}", payload)
        val httpClient = HttpClient.newBuilder().build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString()).let { it ->

            if (it.statusCode() != 200) {
                throw BusinessException("Problem updating custom field in ClickUp")
            }

            return "success"
        }
    }


    fun getUpcomingAndOverdueTasks(): List<Tasks.Task> {

        val params = HashMap<String, String>()
        params["archived"] = "false"
        params["order_by"] = "due_date"
        params["reverse"] = "true"
        val tasks = getTaskRequest(PAYMENT_SCHEDULE_LIST_ID, params)

        val today = LocalDate.now()

        val overdueTasks = tasks.tasks.filter {
            val days = ChronoUnit.DAYS.between(today, it.dueDate.toLocalDate())

            return@filter days <= taskConfigProperties.upcomingTasksDays
        }

        return overdueTasks
    }

    fun getTaskRequest(listId: String, params: Map<String, String>): Tasks {
        val request = requestHelper.getRequest("api/v2/list/$listId/task", params)

        val httpClient = HttpClient.newBuilder().build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString()).let { it ->

            if (it.statusCode() != 200) {
                throw BusinessException("Problem getting tasks from ClickUp")
            }

            return objectMapper.readValue<Tasks>(it.body())
        }
    }

    @Component
    class RequestHelper(
        private val config: ConfigProperties,
        @Qualifier("objectMapper") private val objectMapper: ObjectMapper
    ) {

        fun getRequest(url: String, params: Map<String, String>): HttpRequest {

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

        fun putRequest(url: String, requestBody: Map<String, String>): HttpRequest {

            val payload = objectMapper.writeValueAsString(requestBody)

            HttpRequest.newBuilder().PUT(HttpRequest.BodyPublishers.ofString(payload)).let {
                return buildRequest(it, url)
            }
        }

        fun postRequest(url: String, requestBody: Map<String, String>): HttpRequest {

            val payload = objectMapper.writeValueAsString(requestBody)

            HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(payload)).let {
                return buildRequest(it, url)
            }
        }

        fun buildRequest(builder: HttpRequest.Builder, url: String): HttpRequest {

            return builder
                .uri(URI.create("${config.endpoint}/$url"))
                .header("Content-Type", "application/json")
                .header("Authorization", config.authenticationToken)
                .build()
        }
    }

}
