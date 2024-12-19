package ui.clickupservice.taskreminder.data

import java.math.BigDecimal

data class LoanTask(val task: Tasks.Task, val loan: BigDecimal, val payment: BigDecimal) : ExpenseTask {

    override fun getWrappedTask(): Tasks.Task {
        return task
    }

    override fun getPaymentAmount(): BigDecimal {
        return payment
    }
}
