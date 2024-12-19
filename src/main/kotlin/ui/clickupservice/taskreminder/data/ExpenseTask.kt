package ui.clickupservice.taskreminder.data

import java.math.BigDecimal

interface ExpenseTask {

    fun getWrappedTask(): Tasks.Task

    fun getPaymentAmount(): BigDecimal


}