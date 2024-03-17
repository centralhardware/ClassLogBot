package me.centralhardware.znatoki.telegram.statistic

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

fun LocalDateTime.startOfMonth(): LocalDateTime =
    this.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0)
fun LocalDateTime.endOfMonth(): LocalDateTime =
    this.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59)

fun LocalDateTime.prevMonth(): LocalDateTime = this.minusMonths(1)

fun LocalDateTime.formatDateTime(): String = this.format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm"))
fun LocalDateTime.formatDate(): String = this.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

fun String?.parseDateTime(): Result<LocalDateTime> =
    runCatching { LocalDateTime.parse(this, DateTimeFormatter.ofPattern("dd MM yyyy HH;mm")) }
fun String?.parseDate(): Result<LocalDateTime> =
    runCatching { LocalDateTime.parse(this, DateTimeFormatter.ofPattern("dd MM yyyy")) }