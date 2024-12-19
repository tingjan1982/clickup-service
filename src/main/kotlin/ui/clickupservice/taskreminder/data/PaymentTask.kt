package ui.clickupservice.taskreminder.data

import java.math.BigDecimal

data class PaymentTask(val task: Tasks.Task, val type: Type, val payment: BigDecimal) : ExpenseTask {

    companion object {
        fun toPaymentTask(task: Tasks.Task): PaymentTask {

            val paymentField = task.customFields.first { it.name == "Payment" }.toBigDecimal()
            val type = task.customFields.first { it.name == "Type" }.toEnumType<Type>(Type.NA)

            return PaymentTask(task, type, paymentField)
        }
    }

    override fun getWrappedTask(): Tasks.Task {
        return task
    }

    override fun getPaymentAmount(): BigDecimal {
        return payment
    }


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