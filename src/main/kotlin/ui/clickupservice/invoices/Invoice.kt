package ui.clickupservice.invoices

import java.math.BigDecimal

data class Invoice(
    val invoiceNumber: String,
    val invoiceDate: java.time.LocalDate,
    val entity: String,
    val description: String,
    val amount: BigDecimal
)
