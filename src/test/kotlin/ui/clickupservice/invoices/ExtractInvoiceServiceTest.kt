package ui.clickupservice.invoices

import org.junit.jupiter.api.Test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File

@SpringBootTest
class ExtractInvoiceServiceTest(@Autowired val service: ExtractInvoiceService) {

    @Test
    fun extractInvoiceFromPDF() {

        val file = File("${System.getProperty("user.home")}/Downloads/Invoice no. 41049 - RBG Lawyers Tax Invoice - email.pdf")

        service.extractInvoiceFromPDF(file).also {
            println(it)
        }
    }
}