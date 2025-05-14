package ui.clickupservice.bankexport.data

import java.math.BigDecimal
import java.time.LocalDate

data class BankAccount(
    val account: String,
    val entity: String,
    val balance: BigDecimal,
    val date: LocalDate
)