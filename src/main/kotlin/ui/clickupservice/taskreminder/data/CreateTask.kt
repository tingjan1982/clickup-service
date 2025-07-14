package ui.clickupservice.taskreminder.data

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class CreateTask(val name: String,
                      @JsonProperty(value = "due_date")
                      val dueDate: Date,
                      @JsonProperty(value = "due_date_time")
                      val dueDateTime: Boolean = true,
                      val parent: String,
                      @JsonProperty("custom_fields")
                      val customFields: List<Tasks.Task.CustomField>)