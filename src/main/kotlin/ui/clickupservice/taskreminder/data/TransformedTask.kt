package ui.clickupservice.taskreminder.data

import java.math.BigDecimal

data class TransformedTask(val task: Tasks.Task, val payment: BigDecimal)