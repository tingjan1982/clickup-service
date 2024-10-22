package ui.clickupservice.emailservice

import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import org.springframework.stereotype.Service
import ui.clickupservice.shared.config.ConfigProperties

@Service
class EmailService(val configProperties: ConfigProperties) {

    fun sendEmailToGroup(subject: String, contentStr: String): Response {

        val from = Email("joelin@rabybayharbour.com")
        val to = Email("joelin@rabybayharbour.com")
        val content = Content("text/plain", contentStr)
        val mail = Mail(from, subject, to, content)

        val sg = SendGrid(configProperties.sendgridApiKey)

        val request = Request()
        request.method = Method.POST
        request.endpoint = "mail/send"
        request.body = mail.build()

        val response: Response = sg.api(request)
        println(response.statusCode)
        println(response.body)
        println(response.headers)

        return response
    }
}