package ui.clickupservice.leasing.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertEquals

@SpringBootTest
class LeasingServiceTest(@Autowired private val service: LeasingService) {

    @Test
    fun calculatePercentageRent() {

        service.calculatePercentageRent(BigDecimal(88620), BigDecimal(4)).also {
            assertEquals(BigDecimal("92164.80"), it)
        }
    }

    @Test
    fun calculateCpiRent() {

        val rent = service.calculateCpiRent(BigDecimal(79152), LocalDate.of(2024, Month.DECEMBER, 20))
        println(rent)
    }
}