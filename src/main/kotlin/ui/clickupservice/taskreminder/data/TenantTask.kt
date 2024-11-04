package ui.clickupservice.taskreminder.data

import java.math.BigDecimal
import java.time.LocalDate

data class TenantTask(
    val task: Tasks.Task,
    val rent: BigDecimal,
    val newRent: BigDecimal,
    val monthlyIncentive: BigDecimal,
    val reviewType: ReviewType,
    val anniversaryDate: LocalDate
) {


    enum class ReviewType(val needReview: Boolean = true) {

        CPI, PERCENTAGE, MANUAL(false), NA(false)
    }
}