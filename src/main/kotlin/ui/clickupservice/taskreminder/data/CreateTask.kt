package ui.clickupservice.taskreminder.data

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class CreateTask(val name: String,
                      @param:JsonProperty(value = "due_date")
                      val dueDate: Date,
                      @param:JsonProperty(value = "due_date_time")
                      val dueDateTime: Boolean = true,
                      val parent: String,
                      val tags: List<String>,
                      @param:JsonProperty("custom_fields")
                      val customFields: List<Tasks.Task.CustomField>)
