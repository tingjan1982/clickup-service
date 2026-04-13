package ui.clickupservice.notion.data

import java.math.BigDecimal
import java.time.LocalDate

data class Lease(
    val id: String,
    val location: Location,
    val status: String,
    val tenant: String,
    val leaseDate: LocalDate?,
    val rentReviewDate: LocalDate?,
    val startingRent: BigDecimal?,
    val reviewType: String,
    val rentReviews: MutableList<RentReview> = mutableListOf()
) {

    enum class Location(val locationName: String) {

        HARBOUR("Harbour"),
        BANKSIA_BEACH("Banksia Beach"),
        FAIRCLOTH("Faircloth");
    }
}
