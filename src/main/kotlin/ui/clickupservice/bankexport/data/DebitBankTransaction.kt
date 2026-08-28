package ui.clickupservice.bankexport.data

import java.math.BigDecimal
import java.time.LocalDate

data class DebitBankTransaction(
    val account: String,
    val entity: String,
    val debitAmount: BigDecimal,
    val creditAmount: BigDecimal = BigDecimal.ZERO,
    val date: LocalDate
) {
    val amount: BigDecimal
        get() = if (debitAmount > BigDecimal.ZERO) debitAmount else creditAmount.negate()
}
