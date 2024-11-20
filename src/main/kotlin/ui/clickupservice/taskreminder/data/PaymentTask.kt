package ui.clickupservice.taskreminder.data

import java.math.BigDecimal

data class PaymentTask(val task: Tasks.Task, val type: Type, val payment: BigDecimal) {

    enum class Type(val displayName: String) {

        ATO("ATO"),
        ASIC("ASIC"),
        EXPENSE("Expense"),
        INSURANCE("Insurance"),
        LAND_RENT("Land rent"),
        LAND_TAX("Land tax"),
        LEVY("Levy"),
        RATE("Rate"), NA("")
    }
}