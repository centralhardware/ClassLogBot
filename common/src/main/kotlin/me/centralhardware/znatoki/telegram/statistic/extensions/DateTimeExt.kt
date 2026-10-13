package me.centralhardware.znatoki.telegram.statistic.extensions

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

fun LocalDateTime.startOfDay(): LocalDateTime = with(LocalTime.MIN)

fun LocalDateTime.startOfMonth(): LocalDateTime =
    with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN)

/** Exclusive upper bound of this month: midnight on the 1st of the next month. */
fun LocalDateTime.startOfNextMonth(): LocalDateTime = startOfMonth().plusMonths(1)

fun LocalDateTime.prevMonth(): LocalDateTime = minusMonths(1)

fun LocalDateTime.formatDateTime(): String = format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm"))

fun LocalDateTime.formatDate(): String = format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))


fun LocalDate.formatDate(): String = format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

fun LocalDateTime.formatTime(): String = format(DateTimeFormatter.ofPattern("HH:mm"))


fun String?.parseDate(): Result<LocalDate> = runCatching {
    LocalDate.parse(this, DateTimeFormatter.ofPattern("dd MM yyyy"))
}

fun LocalDateTime.isInSameMonthAs(other: LocalDateTime): Boolean =
    this.year == other.year && this.month == other.month
