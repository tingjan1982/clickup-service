package ui.clickupservice.leasing.service

import org.springframework.stereotype.Service
import ui.clickupservice.leasing.data.LeasingConfigProperties
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month


@Service
class LeasingService(val leasingConfigProperties: LeasingConfigProperties) {

    fun calculatePercentageRent(rent: BigDecimal, percentage: BigDecimal): BigDecimal {

        return rent.multiply(percentage.divide(BigDecimal(100)).add(BigDecimal.ONE))
    }

    fun calculateCpiRent(rent: BigDecimal, anniversary: LocalDate): BigDecimal {

        val cpiFigure = getCpiFigure(anniversary)
        return rent.multiply(cpiFigure.toBigDecimal())
    }

    internal fun getCpiFigure(anniversary: LocalDate): Double {

        val year = anniversary.year
        val cpiFigures = leasingConfigProperties.cpiFigures

        val quarter = when (anniversary.month) {
            Month.JANUARY, Month.FEBRUARY, Month.MARCH -> Pair(year - 1, 4)
            Month.APRIL, Month.MAY, Month.JUNE -> Pair(year, 1)
            Month.JULY, Month.AUGUST, Month.SEPTEMBER -> Pair(year, 2)
            Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER -> Pair(year, 3)
            else -> throw Exception("Include all months in the conditions")
        }

        val currentCpi = cpiFigures["${quarter.first}-${quarter.second}"] ?: throw Exception("Current CPI is not available yet")
        val prevCpi = cpiFigures["${quarter.first - 1}-${quarter.second}"] ?: throw Exception("Prev CPI is not available yet")
        return currentCpi / prevCpi
    }


}