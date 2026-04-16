package ui.clickupservice.emailservice

import com.sendgrid.Response
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val sendGridEmailSender: SendGridEmailSender,
    private val brevoEmailSender: BrevoEmailSender
) {

    fun sendPlainEmail(subject: String, contentStr: String): Response {
        return sendGridEmailSender.sendPlainEmail(subject, contentStr)
    }

    fun sendDynamicEmail(subject: String, contentStr: String, dynamicData: Map<String, String> = mapOf()): Response {
        return sendGridEmailSender.sendDynamicEmail(subject, contentStr, dynamicData)
    }

    fun sendBrevoEmail(subject: String, contentStr: String, recipientEmail: String? = null): String {
        return brevoEmailSender.sendBrevoEmail(subject, contentStr, recipientEmail)
    }

    fun sendBrevoTemplateEmail(
        subject: String,
        contentStr: String,
        templateId: Long = 1,
        dynamicData: Map<String, Any> = mapOf()
    ): String {
        return brevoEmailSender.sendBrevoTemplateEmail(subject, contentStr, templateId, dynamicData)
    }
}
