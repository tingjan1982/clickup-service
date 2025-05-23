package ui.clickupservice.emailservice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class EmailServiceTest(@Autowired val emailService: EmailService) {

    @Test
    fun sendPlainEmail() {

        emailService.sendPlainEmail("Sendgrid Test", "Sendgrid email success").let {
            assertEquals(202, it.statusCode)
        }
    }
}