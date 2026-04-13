package ui.clickupservice.taskreminder.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TenantServiceTest(@Autowired val tenantService: TenantService) {

    @Test
    fun sendRentReviewReminder() {

        tenantService.sendRentReviewReminder()
    }

    @Test
    fun updateTenantsInRentReview() {

        tenantService.updateTenantsInRentReview()
    }

    @Test
    fun sendTenantsInRentReview() {

        tenantService.sendTenantsInRentReview()
    }

}