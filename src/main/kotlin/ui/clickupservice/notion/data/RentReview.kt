package ui.clickupservice.notion.data

import java.math.BigDecimal

data class RentReview(
    val year: Int,
    val leases: String,
    val newRent: BigDecimal = BigDecimal.ZERO,
    val adoptedCPI: BigDecimal = BigDecimal.ZERO,
    var awaitCPI: Boolean = false
)
