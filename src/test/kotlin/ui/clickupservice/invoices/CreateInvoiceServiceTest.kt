package ui.clickupservice.invoices

import org.junit.jupiter.api.Test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CreateInvoiceServiceTest(@Autowired val service: CreateInvoiceService) {

    @Test
    fun writeInvoicesFromGoogleDrive() {

        service.writeInvoicesFromGoogleDrive()
    }
}