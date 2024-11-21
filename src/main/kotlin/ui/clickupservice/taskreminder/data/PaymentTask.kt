package ui.clickupservice.taskreminder.data

import java.math.BigDecimal

data class PaymentTask(val task: Tasks.Task, val type: Type, val payment: BigDecimal) {

    enum class Type(val displayName: String) {

        ASIC("ASIC"),
        ATO("ATO"),
        EXPENSE("Expense"),
        INSURANCE("Insurance"),
        LAND_RENT("Land Rent"),
        LAND_TAX("Land Tax"),
        LEVY("Levy"),
        RATE("Rate"),
        INTEREST("Interest"),
        NA("")
    }
}