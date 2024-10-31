package ui.clickupservice.emailservice

import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.Personalization
import org.springframework.stereotype.Service
import ui.clickupservice.shared.config.ConfigProperties
import ui.clickupservice.shared.exception.BusinessException

/**
 * Reference for SendGrid dynamic template: https://stackoverflow.com/questions/53860093/showing-command-line-output-on-a-html-page/53860562
 */
@Service
class EmailService(val configProperties: ConfigProperties) {

    fun sendPlainEmail(subject: String, contentStr: String): Response {

        val from = getFromEmail()
        val to = Email(configProperties.notificationEmail)
        val content = Content("text/plain", contentStr)
        val mail = Mail(from, subject, to, content)

        return sendEmail(mail)
    }

    fun sendDynamicEmail(subject: String, contentStr: String, dynamicData: Map<String, String> = mapOf()): Response {

        val from = getFromEmail()
        val to = Email(configProperties.notificationEmail)
        val mail = Mail()
        mail.from = from
        mail.subject = subject
        mail.setTemplateId("d-6313f48d216b41f49b3cc4e5b45e2df5")

        val personalization = (Personalization())
        personalization.addTo(to)
        personalization.addDynamicTemplateData("subject", "[Automated] $subject")
        personalization.addDynamicTemplateData("content", contentStr)

        dynamicData.forEach {
            personalization.addDynamicTemplateData(it.key, it.value)
        }

        mail.addPersonalization(personalization)

        val response: Response = sendEmail(mail)

        return response
    }

    private fun getFromEmail(): Email {
        val from = Email("joelin@rabybayharbour.com")
        from.name = "Joe's Reminder"
        return from
    }

    private fun sendEmail(mail: Mail): Response {
        val sg = SendGrid(configProperties.sendgridApiKey)

        val request = Request()
        request.method = Method.POST
        request.endpoint = "mail/send"
        request.body = mail.build()

        val response: Response = sg.api(request)

        if (response.statusCode != 202) {
            throw BusinessException("Sending email notification failed: ${response.body}")
        }

        println(response.statusCode)
        println(response.body)
        println(response.headers)
        return response
    }
}