package ui.clickupservice.leasing.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

@SpringBootTest
class LeasingServiceTest(@Autowired private val service: LeasingService) {

    @Test
    fun calculateRent() {

        val rent = service.calculateRent(BigDecimal(79152), LocalDate.of(2024, Month.DECEMBER, 20))
        println(rent)
    }
}