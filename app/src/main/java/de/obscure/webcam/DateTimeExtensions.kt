package de.obscure.webcam

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

object DateTimeExtensions {

    private val dateTimeFormatDayMonth = DateTimeFormat.forPattern("dd.MM")
    private val dateTimeFormatLong = DateTimeFormat.forPattern("dd.MM.yyyy 'um' HH.mm 'Uhr'")

    fun DateTime.forGermany(): DateTime {
        return this.withZone(DateTimeZone.forID("Europe/Berlin"))
    }

    fun DateTime.printGermanDayMonthFormat(): String {
        return dateTimeFormatDayMonth.print(this)
    }

    fun DateTime.printGermanLongFormat(): String {
        return dateTimeFormatLong.print(this)
    }

}