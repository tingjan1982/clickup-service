package ui.clickupservice.invoices

import org.junit.jupiter.api.Test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CreatedInvoiceServiceTest(@Autowired val service: CreatedInvoiceService) {

    @Test
    fun readGoogleDriveFile() {

        service.readGoogleDriveFile()
    }
}