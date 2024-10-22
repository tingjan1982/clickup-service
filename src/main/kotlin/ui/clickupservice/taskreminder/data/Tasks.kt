package ui.clickupservice.taskreminder.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import java.util.Date

//@JsonIgnoreProperties(ignoreUnknown = true)
data class Tasks(var tasks: List<Task>) {

    //@JsonIgnoreProperties(ignoreUnknown = true)
    data class Task(
        val id: String,
        val name: String,
        @JsonProperty("due_date")
        //@JsonDeserialize(using = LocalDateDeserializer::class)
        var dueDate: Date,
        var status1: String?,
        var tags: List<Tag>
    ) {
        data class Tag(
            val name: String
        )

        @JsonProperty("status")
        private fun unpackNameFromNestedObject(statusObj: Map<String, String>) {
            status1 = statusObj["status"]
        }
    }
}