package ui.clickupservice.emailservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Service
import ui.clickupservice.shared.config.ConfigProperties
import ui.clickupservice.shared.exception.BusinessException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate

@Service
class BrevoEmailSender(private val configProperties: ConfigProperties) {
    private val objectMapper = jacksonObjectMapper()

    fun sendBrevoEmail(subject: String, contentStr: String): String {

        val payload = BrevoEmailRequest(
            sender = BrevoAddress(email = "joelin@rabybayharbour.com", name = "Joe's Reminder"),
            to = listOf(BrevoAddress(email = configProperties.notificationEmail)),
            subject = subject,
            textContent = contentStr
        )

        return sendBrevoRequest(payload)
    }

    fun sendBrevoTemplateEmail(
        subject: String,
        contentStr: String,
        templateId: Long = 1,
        dynamicData: Map<String, Any> = mapOf()
    ): String {

        val defaultParams = mapOf(
            "subject" to subject,
            "content" to contentStr,
            "appName" to "clickup-service",
            "year" to LocalDate.now().year
        )

        val payload = BrevoTemplateEmailRequest(
            sender = BrevoAddress(email = "joelin@rabybayharbour.com", name = "Joe's Reminder"),
            to = listOf(BrevoAddress(email = configProperties.notificationEmail)),
            templateId = templateId,
            params = defaultParams + dynamicData
        )

        return sendBrevoRequest(payload)
    }

    private fun sendBrevoRequest(payload: Any): String {
        val apiKey = configProperties.brevoApiKey

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
            .header("accept", "application/json")
            .header("api-key", apiKey)
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build()

        val response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in setOf(200, 201, 202)) {
            throw BusinessException("Sending email notification failed via Brevo: ${response.body()}")
        }

        return response.body()
    }

    private data class BrevoEmailRequest(
        val sender: BrevoAddress,
        val to: List<BrevoAddress>,
        val subject: String,
        val textContent: String
    )

    private data class BrevoTemplateEmailRequest(
        val sender: BrevoAddress,
        val to: List<BrevoAddress>,
        val templateId: Long,
        val params: Map<String, Any>
    )

    private data class BrevoAddress(
        val email: String,
        val name: String? = null
    )
}
