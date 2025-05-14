package ui.clickupservice.bankexport.data

import java.math.BigDecimal
import java.time.LocalDate

data class DebitBankTransaction(
    val account: String,
    val entity: String,
    val debitAmount: BigDecimal,
    val date: LocalDate
)
