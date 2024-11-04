package ui.clickupservice.taskreminder.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import java.math.BigDecimal
import java.util.*
import kotlin.enums.enumEntries

//@JsonIgnoreProperties(ignoreUnknown = true)
data class Tasks(var tasks: List<Task>) {

    data class Task(
        val id: String,
        val name: String,
        @JsonProperty(value = "start_date", required = false)
        @JsonSetter(nulls = Nulls.SKIP)
        var startDate: Date = Date(),
        @JsonProperty(value = "due_date", required = false)
        @JsonSetter(nulls = Nulls.SKIP)
        //@JsonDeserialize(using = LocalDateDeserializer::class)
        var dueDate: Date = Date(),
        var taskStatus: String?,
        var tags: List<Tag>,
        @JsonProperty("custom_fields")
        var customFields: List<CustomField>
    ) {
        fun toTagString(): String {
            return tags.joinToString { it.name }
        }


        data class Tag(
            val name: String
        )

        data class CustomField(
            val id: String,
            val name: String,
            @JsonProperty(required = false)
            val value: String?
        ) {

            fun toBigDecimal(): BigDecimal {
                return if (value != null) BigDecimal(value) else BigDecimal.ZERO
            }

            @OptIn(ExperimentalStdlibApi::class)
            inline fun <reified E : Enum<E>> toEnumType(defaultValue: E): E {

                return value?.let {
                    enumEntries<E>().filterIndexed { index, _ -> index == value.toInt() }.first()
                } ?: defaultValue


                //return if (value != null && value != "0") enumValueOf<E>(value) else defaultValue
            }
        }

        @JsonProperty("status")
        private fun unpackNameFromNestedObject(statusObj: Map<String, String>) {
            taskStatus = statusObj["status"]
        }
    }
}