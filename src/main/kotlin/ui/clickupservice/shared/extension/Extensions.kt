package ui.clickupservice.shared.extension

import ui.clickupservice.shared.extension.Extensions.Companion.df
import ui.clickupservice.shared.extension.Extensions.Companion.dtf
import ui.clickupservice.shared.extension.Extensions.Companion.nf
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.util.*

class Extensions {
    companion object {
        val df = SimpleDateFormat("dd/MM/yyyy")
        val dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        val nf = DecimalFormat("#.##")

        fun getSimpleDate(year: Int, month: Month, day: Int): Date {

            val anniversary = LocalDate.of(year, month, day).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()
            return Date.from(anniversary)
        }


    }
}

fun Date.toDateFormat(): String {
    return df.format(this)
}

fun Date.toLocalDate(): LocalDate {
    return LocalDate.ofInstant(this.toInstant(), ZoneId.systemDefault())
}

fun LocalDate.toDateFormat(): String {
    return this.format(dtf)
}

fun LocalDate.getQuarter(): Int {
    return this.get(IsoFields.QUARTER_OF_YEAR)
}

fun LocalDate.getLastQuarter(): Int {
    return when (this.getQuarter()) {
        1 -> 4
        2 -> 1
        3 -> 2
        4 -> 3
        else -> throw Exception("It will never get here")
    }
}

fun LocalDate.isInReviewPeriod(): Boolean {
    val now = LocalDate.now()
    val currentQuarter = now.getQuarter()

    return this.getQuarter() == currentQuarter
}

fun BigDecimal.formatNumber(): String {
    return nf.format(this)
}