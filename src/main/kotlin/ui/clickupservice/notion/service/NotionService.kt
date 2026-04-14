package ui.clickupservice.notion.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import ui.clickupservice.notion.data.Lease
import ui.clickupservice.notion.data.RentReview
import ui.clickupservice.shared.config.ConfigProperties
import ui.clickupservice.shared.exception.BusinessException
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParsePosition
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Service
class NotionService(
    private val configProperties: ConfigProperties,
    private val objectMapper: ObjectMapper
) {
    private val decimalFormat = DecimalFormat("#,##0.################", DecimalFormatSymbols.getInstance(Locale.US)).apply {
        isParseBigDecimal = true
    }

    companion object {
        const val LEASE_DATABASE_ID = "255f1412e7f980ae9810d96d966a31ab"
        const val RENT_REVIEW_DATABASE_ID = "330f1412e7f980b0a967e60096d7d631"
    }

    fun queryDatabase(databaseId: String, filter: FieldFilter? = null): NotionQueryResponse {

        val notionApiKey = configProperties.notionApiKey
        val requestBody = buildQueryBody(filter)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.notion.com/v1/databases/$databaseId/query"))
            .header("Authorization", "Bearer $notionApiKey")
            .header("Notion-Version", "2022-06-28")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .build()

        val response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in setOf(200, 201)) {
            throw BusinessException("Failed to query Notion database: ${response.body()}")
        }

        val body = objectMapper.readValue(response.body(), NotionDatabaseQueryApiResponse::class.java)

        return NotionQueryResponse(
            databaseId = databaseId,
            results = body.results,
            hasMore = body.hasMore,
            nextCursor = body.nextCursor
        )
    }

    private fun buildQueryBody(filter: FieldFilter?): Map<String, Any> {
        if (filter == null) {
            return emptyMap()
        }

        if (filter.type == FilterType.NUMBER && filter.value.toBigDecimalOrNull() == null) {
            throw BusinessException("Invalid number filter value: ${filter.value}")
        }

        val condition = when (filter.type) {
            FilterType.STATUS -> mapOf("equals" to filter.value)
            FilterType.SELECT -> mapOf("equals" to filter.value)
            FilterType.RICH_TEXT -> mapOf("contains" to filter.value)
            FilterType.TITLE -> mapOf("contains" to filter.value)
            FilterType.NUMBER -> mapOf("equals" to filter.value.toBigDecimal())
            FilterType.DATE -> mapOf("equals" to filter.value)
            FilterType.FORMULA -> mapOf("string" to mapOf("contains" to filter.value))
        }

        return mapOf(
            "filter" to mapOf(
                "property" to filter.field,
                filter.type.apiKey to condition
            )
        )
    }

    fun readRentReview(): Map<String, List<RentReview>> {
        val response = queryDatabase(RENT_REVIEW_DATABASE_ID)

        val rentReviews = response.results.map { page ->
            val properties = page.properties
            val yearProperty = findProperty(properties, "Year")
            val leasesProperty = findProperty(properties, "Leases")
            val newRentProperty = findProperty(properties, "New Rent")
            val adoptedCpiProperty = findProperty(properties, "Adopted CPI")

            RentReview(
                year = extractYear(yearProperty),
                leases = extractRelation(leasesProperty),
                newRent = extractNumber(newRentProperty) ?: BigDecimal.ZERO,
                adoptedCPI = extractNumber(adoptedCpiProperty) ?: BigDecimal.ZERO
            )
        }

        return rentReviews.groupBy { it.leases }
    }

    fun readLeases(location: Lease.Location): List<Lease> {

        val response = queryDatabase(LEASE_DATABASE_ID, FieldFilter("Location", location.locationName, FilterType.SELECT))

        return response.results.map { page ->
            val properties = page.properties
            val locationProperty = findProperty(properties, "Location")
            val statusProperty = findProperty(properties, "Status")
            val tenantProperty = findProperty(properties, "Tenant")
            val leaseDateProperty = findProperty(properties, "Lease Date")
            val rentReviewDateProperty = findProperty(properties, "Rent Review Date")
            val startingRentProperty = findProperty(properties, "Starting Rent")
            val reviewType = findProperty(properties, "Review Type")

            Lease(
                id = page.id,
                location = extractLocation(locationProperty),
                status = extractPropertyText(statusProperty),
                tenant = extractPropertyText(tenantProperty),
                leaseDate = extractDate(leaseDateProperty) ?: LocalDate.now(),
                rentReviewDate = extractDate(rentReviewDateProperty) ?: LocalDate.now(),
                startingRent = extractNumber(startingRentProperty) ?: BigDecimal.ZERO,
                reviewType = extractPropertyText(reviewType)
            )
        }
    }

    private fun findProperty(properties: Map<String, NotionProperty>, vararg candidates: String): NotionProperty {
        val firstMatch = candidates.firstNotNullOfOrNull { properties[it] }
        if (firstMatch != null) {
            return firstMatch
        }

        val normalizedCandidates = candidates.map { it.normalizePropertyKey() }.toSet()

        return properties.entries.firstOrNull { (key, _) ->
            key.normalizePropertyKey() in normalizedCandidates
        }?.value ?: throw BusinessException(
            "Notion field not found. Expected one of: ${candidates.joinToString()}. Available fields: ${properties.keys.joinToString()}"
        )
    }

    private fun extractPropertyText(property: NotionProperty): String {

        if (property.title.isNotEmpty()) {
            return property.title.joinToString("") { it.plainText.orEmpty() }.trim()
        }

        if (property.richText.isNotEmpty()) {
            return property.richText.joinToString("") { it.plainText.orEmpty() }.trim()
        }

        property.status?.name?.takeIf { it.isNotBlank() }?.let { return it }
        property.select?.name?.takeIf { it.isNotBlank() }?.let { return it }
        property.rollup?.let { rollup ->
            when (rollup.type) {
                "number" -> rollup.number?.toPlainString()?.let { return it }
                "date" -> rollup.date?.start?.let { return it }
                "array" -> if (rollup.array.isNotEmpty()) {
                    return rollup.array.joinToString(",") { extractPropertyText(it) }.trim()
                }
            }
        }
        property.name?.takeIf { it.isNotBlank() }?.let { return it }

        return ""
    }

    private fun extractNumber(property: NotionProperty): BigDecimal? {

        property.number?.let { return it }
        property.formula?.number?.let { return it }
        property.rollup?.number?.let { return it }
        property.formula?.string?.let { parseBigDecimal(it)?.let { parsed -> return parsed } }
        property.rollup?.array?.forEach { rollupProperty ->
            extractNumber(rollupProperty)?.let { return it }
        }

        return parseBigDecimal(extractPropertyText(property))
    }

    private fun extractYear(property: NotionProperty): Int {

        property.number?.toInt()?.let { return it }
        property.formula?.number?.toInt()?.let { return it }
        property.rollup?.number?.toInt()?.let { return it }
        extractDate(property)?.year?.let { return it }

        return extractPropertyText(property).trim().toInt()
    }

    private fun extractRelation(property: NotionProperty): String {

        if (property.relation.isNotEmpty()) {
            return property.relation.joinToString(",") { it.id }.trim()
        }

        return extractPropertyText(property)
    }

    private fun extractDate(property: NotionProperty): LocalDate? {

        return extractLocalDate(property.date?.start)
            ?: extractLocalDate(property.formula?.date?.start)
            ?: extractLocalDate(property.rollup?.date?.start)
            ?: extractLocalDate(property.formula?.string)
            ?: extractLocalDate(property.rollup?.array?.asSequence()?.map { extractPropertyText(it) }?.firstOrNull { it.isNotBlank() })
            ?: extractLocalDate(extractPropertyText(property))
    }

    private fun extractLocation(property: NotionProperty): Lease.Location {

        val locationString = extractPropertyText(property)
        val normalized = locationString.normalizePropertyKey()

        return when (locationString) {
            "Harbour" -> Lease.Location.HARBOUR
            "Banksia Beach" -> Lease.Location.BANKSIA_BEACH
            "Faircloth" -> Lease.Location.FAIRCLOTH
            else -> throw BusinessException(
                "Unknown lease location '$locationString'. Supported values: HARBOUR, BANKSIA_BEACH, FAIRCLOTH"
            )
        }
    }

    private fun extractLocalDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) {
            return null
        }

        return runCatching { LocalDate.parse(value) }
            .getOrElse { runCatching { OffsetDateTime.parse(value).toLocalDate() }.getOrNull() }
    }

    private fun parseBigDecimal(raw: String?): BigDecimal? {
        if (raw.isNullOrBlank()) {
            return null
        }

        val trimmed = raw.trim()
        val isNegativeByBrackets = trimmed.startsWith("(") && trimmed.endsWith(")")
        val normalized = trimmed
            .replace("[()]".toRegex(), "")
            .replace("[^0-9,.-]".toRegex(), "")

        if (normalized.isBlank() || normalized == "-" || normalized == "." || normalized == ",") {
            return null
        }

        val parsePosition = ParsePosition(0)
        val parsed = decimalFormat.parse(normalized, parsePosition)
        if (parsePosition.index != normalized.length || parsed !is BigDecimal) {
            return null
        }

        val number = parsed
        return if (isNegativeByBrackets) number.negate() else number
    }

    data class NotionQueryResponse(
        val databaseId: String,
        val results: List<NotionPage>,
        val hasMore: Boolean,
        val nextCursor: String?
    )

    data class FieldFilter(
        val field: String,
        val value: String,
        val type: FilterType = FilterType.RICH_TEXT
    )

    enum class FilterType(val apiKey: String) {
        STATUS("status"),
        SELECT("select"),
        RICH_TEXT("rich_text"),
        TITLE("title"),
        NUMBER("number"),
        DATE("date"),
        FORMULA("formula")
    }

    data class NotionDatabaseQueryApiResponse(
        val results: List<NotionPage> = emptyList(),
        @param:JsonProperty("has_more") val hasMore: Boolean = false,
        @param:JsonProperty("next_cursor") val nextCursor: String? = null
    )

    data class NotionPage(
        val id: String,
        val properties: Map<String, NotionProperty> = emptyMap()
    )

    data class NotionProperty(
        val title: List<NotionTextToken> = emptyList(),
        @param:JsonProperty("rich_text") val richText: List<NotionTextToken> = emptyList(),
        val status: NotionNamedValue? = null,
        val select: NotionNamedValue? = null,
        val relation: List<NotionRelationValue> = emptyList(),
        val date: NotionDateValue? = null,
        val formula: NotionFormulaValue? = null,
        val rollup: NotionRollupValue? = null,
        val number: BigDecimal? = null,
        val name: String? = null
    )

    data class NotionTextToken(
        @param:JsonProperty("plain_text") val plainText: String? = null
    )

    data class NotionNamedValue(
        val name: String? = null
    )

    data class NotionDateValue(
        val start: String? = null,
        val end: String? = null
    )

    data class NotionFormulaValue(
        val type: String? = null,
        val string: String? = null,
        val number: BigDecimal? = null,
        val boolean: Boolean? = null,
        val date: NotionDateValue? = null
    )

    data class NotionRollupValue(
        val type: String? = null,
        val number: BigDecimal? = null,
        val date: NotionDateValue? = null,
        val array: List<NotionProperty> = emptyList()
    )

    data class NotionRelationValue(
        val id: String
    )

    private fun String.normalizePropertyKey(): String {
        return lowercase().replace("[^a-z0-9]".toRegex(), "")
    }
}
