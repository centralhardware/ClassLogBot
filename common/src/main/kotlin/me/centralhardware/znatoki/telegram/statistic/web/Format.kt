package me.centralhardware.znatoki.telegram.statistic.web

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// How dates, months and money read in audit entries, file names and bot messages.

val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val DMY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val DT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm")

private val MONTHS_GEN = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
private val MONTHS_NOM = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
private val WEEKDAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

fun ruDay(d: LocalDate) = "${d.dayOfMonth} ${MONTHS_GEN[d.monthValue - 1]}"
fun monthTitle(m: YearMonth) = "${MONTHS_NOM[m.monthValue - 1]} ${m.year}"
fun weekdayShort(i: Int) = WEEKDAYS[i]
fun rub(v: Number) = "%,d ₽".format(v.toLong()).replace(',', ' ')
fun fmtPhone(p: String?) = p?.let { "+7 ${it.substring(1, 4)} ${it.substring(4, 7)}-${it.substring(7, 9)}-${it.substring(9)}" }
