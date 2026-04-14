package ui.clickupservice.notion.data

import java.math.BigDecimal
import java.time.LocalDate

data class Lease(
    val id: String,
    val location: Location,
    val status: String,
    val tenant: String,
    val leaseDate: LocalDate = LocalDate.now(),
    val rentReviewDate: LocalDate = LocalDate.now(),
    val startingRent: BigDecimal = BigDecimal.ZERO,
    val reviewType: String,
    val rentReviews: MutableList<RentReview> = mutableListOf()
) {

    enum class Location(val locationName: String) {

        HARBOUR("Harbour"),
        BANKSIA_BEACH("Banksia Beach"),
        FAIRCLOTH("Faircloth");
    }
}
