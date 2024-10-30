package ui.clickupservice.taskreminder.data

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.*

//@JsonIgnoreProperties(ignoreUnknown = true)
data class Tasks(var tasks: List<Task>) {

    data class Task(
        val id: String,
        val name: String,
        @JsonProperty("due_date")
        //@JsonDeserialize(using = LocalDateDeserializer::class)
        var dueDate: Date,
        var taskStatus: String?,
        var tags: List<Tag>,
        @JsonProperty("custom_fields")
        var customFields: List<CustomField>
    ) {
        data class Tag(
            val name: String
        )

        data class CustomField(
            val name: String,
            @JsonProperty(required = false)
            val value: BigDecimal?
        )

        @JsonProperty("status")
        private fun unpackNameFromNestedObject(statusObj: Map<String, String>) {
            taskStatus = statusObj["status"]
        }
    }
}