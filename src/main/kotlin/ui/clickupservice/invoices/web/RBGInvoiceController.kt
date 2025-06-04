package ui.clickupservice.invoices.web

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ui.clickupservice.invoices.CreateInvoiceService
import ui.clickupservice.shared.web.ApiResponse

@RestController
@RequestMapping("/invoices")
class RBGInvoiceController(val createInvoiceService: CreateInvoiceService) {

    @PostMapping("/populateRBGInvoices")
    fun populateRBGInvoices(): ApiResponse {

        createInvoiceService.writeInvoicesFromGoogleDrive().let { count ->
            return ApiResponse("Populated $count RBG invoices")
        }

    }
}