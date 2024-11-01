package ui.clickupservice.shared.extension

import ui.clickupservice.shared.extension.Extensions.Companion.df
import ui.clickupservice.shared.extension.Extensions.Companion.nf
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.*

class Extensions {
    companion object {
        val df = SimpleDateFormat("dd/MM/yyyy")

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

fun BigDecimal.formatNumber(): String {
    return nf.format(this)
}