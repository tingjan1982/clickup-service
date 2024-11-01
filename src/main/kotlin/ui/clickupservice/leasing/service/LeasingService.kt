package ui.clickupservice.leasing.service

import org.springframework.stereotype.Service
import ui.clickupservice.shared.extension.toLocalDate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*

@Service
class LeasingService {

    companion object {
        val cpiFigures: Map<String, Float> = mapOf(
            "2022-4" to 132.1f,
            "2023-1" to 134.6f,
            "2023-2" to 136f,
            "2023-3" to 137f,
            "2023-4" to 137.7f,
            "2024-1" to 139.2f,
            "2024-2" to 140.6f,
            "2024-3" to 139.4f
        )
    }

    fun calculateRent(rent: BigDecimal, anniversary: Date): Pair<BigDecimal, Float> {

        val cpiFigure = getCpiFigure(anniversary)
        return rent.multiply(cpiFigure.toBigDecimal()) to cpiFigure

    }

    internal fun getCpiFigure(date: Date): Float {

        val anniversary = date.toLocalDate()
        val currentYear = LocalDate.now().year

        val quarter = when (anniversary.month) {
            Month.JANUARY, Month.FEBRUARY, Month.MARCH -> Pair(currentYear - 1, 4)
            Month.APRIL, Month.MAY, Month.JUNE -> Pair(currentYear, 1)
            Month.JULY, Month.AUGUST, Month.SEPTEMBER -> Pair(currentYear, 2)
            Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER -> Pair(currentYear, 3)
            else -> throw Exception("Include all months in the conditions")
        }

        val currentCpi = cpiFigures["${quarter.first}-${quarter.second}"] ?: throw Exception("Current CPI is not available yet")
        val prevCpi = cpiFigures["${quarter.first - 1}-${quarter.second}"] ?: throw Exception("Prev CPI is not available yet")
        return currentCpi / prevCpi
    }


}