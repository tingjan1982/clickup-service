package ui.clickupservice.notion.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ui.clickupservice.notion.data.Lease.Location

@SpringBootTest
class NotionServiceTest(@Autowired private val notionService: NotionService) {

    @Test
    fun readLeases() {

        val rentReviews = notionService.readRentReview()

        val leases = notionService.readLeases(Location.HARBOUR).map {

            rentReviews[it.id]?.let { reviews ->
                it.rentReviews.addAll(reviews)
            }

            return@map it
        }

        leases.forEach { lease ->
            println(lease)
        }
    }
}
