package ui.clickupservice.statement

import com.google.api.client.util.Value
import java.math.BigDecimal
import java.time.LocalDate

data class Transaction(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val transactionType: TransactionType,
    val dueDate: LocalDate,
    val account: String
) {

    enum class TransactionType {

        @Value("DEBIT")
        DEBIT,

        @Value("CREDIT")
        CREDIT
    }
}
