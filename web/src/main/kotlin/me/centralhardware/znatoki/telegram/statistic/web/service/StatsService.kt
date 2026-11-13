package me.centralhardware.znatoki.telegram.statistic.web.service

import kotliquery.Session
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.web.*
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

/**
 * "Итоги": per-month totals for the chosen period plus the period before it (for the deltas); for
 * admins and the director also the per-tutor table, a forecast for the rest of the month and where
 * students come from and why they leave. Tutors get their own current month only.
 */
object StatsService {

    context(_: Session)
    fun stats(c: Caller, requestedMonths: Int?): StatsDto {
        val n = if (c.seesAll) (requestedMonths ?: 6).coerceIn(1, 12) else 1
        val scope = if (c.seesAll) null else c.id
        val now = YearMonth.now()
        val from = now.minusMonths((if (c.seesAll) 2L * n else 1L) - 1)
        val lessons = WebRepo.lessons(from.start(), now.end(), scope).filterNot { it.deleted }
        val payments = WebRepo.payments(from.start(), now.end(), scope).filterNot { it.deleted }
        val current = lessons.filter { it.month == now }
        return StatsDto(
            months = generateSequence(from) { it.plusMonths(1) }.takeWhile { !it.isAfter(now) }.map { month(it, lessons, payments) }.toList(),
            weekday = lessons.filter { !it.month.isBefore(now.minusMonths(n - 1L)) }.let { p -> (1..7).map { d -> p.count { it.dateTime.dayOfWeek.value == d } } },
            top = top(current),
            debts = if (c.seesAll) debts(current, payments.filter { it.month == now }) else null,
            admin = if (c.seesAll) admin(lessons, payments, now) else null,
        )
    }

    private fun month(m: YearMonth, lessons: List<LessonGroup>, payments: List<PaymentRow>): MonthAggDto {
        val ls = lessons.filter { it.month == m }
        return MonthAggDto(
            m = m.toString(),
            lessons = ls.size,
            billed = ls.sumOf { it.price * it.students.size },
            received = payments.filter { it.month == m }.sumOf { it.amount.toLong() },
            ind = ls.count { !it.isGroup },
            grp = ls.count { it.isGroup },
            subj = ls.groupingBy { it.subject.toString() }.eachCount(),
            students = ls.flatMap { it.students }.distinct().size,
            partial = m == YearMonth.now(),
        )
    }

    /** The students with the most lessons this month. */
    context(_: Session)
    private fun top(current: List<LessonGroup>): List<CountDto> {
        val names = WebRepo.studentNames()
        return current.flatMap { it.students }.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.take(4)
            .map { CountDto(it.key, names[it.key]?.short ?: "#${it.key}", it.value) }
    }

    /** This month's billed minus paid, per student, where positive. */
    context(_: Session)
    private fun debts(current: List<LessonGroup>, paid: List<PaymentRow>): List<DebtDto> {
        val names = WebRepo.studentNames()
        val billed = mutableMapOf<Int, Double>()
        current.forEach { l -> l.students.forEach { billed[it] = (billed[it] ?: 0.0) + l.price } }
        val paidBy = paid.groupBy { it.student }.mapValues { e -> e.value.sumOf { it.amount.toLong() } }
        return billed.map { (id, b) -> DebtDto(id, names[id]?.short ?: "#$id", b - (paidBy[id] ?: 0)) }
            .filter { it.owe > 0 }.sortedByDescending { it.owe }.take(5)
    }

    context(_: Session)
    private fun admin(lessons: List<LessonGroup>, payments: List<PaymentRow>, now: YearMonth): StatsAdminDto {
        val slots = WebRepo.slots(null, WebRepo.studentNames())
        val students = WebRepo.studentRows(includeDeleted = false, withBalance = false)
        val left = students.filter { it.status == "left" }
        val done = lessons.filter { it.month == now }.sumOf { it.price * it.students.size }
        val rest = scheduledRest(slots, now)
        return StatsAdminDto(
            perTutor = perTutor(lessons, payments, slots, now),
            forecastDone = done,
            // Billed so far plus the schedule's remaining slots, less the usual 7–18% of skipped lessons.
            forecastLo = ((done + rest * 0.82) / 1000).roundToLong(),
            forecastHi = ((done + rest * 0.93) / 1000).roundToLong(),
            active = students.count { it.status == "active" },
            paused = students.count { it.status == "paused" },
            left = left.size,
            reasons = left.groupingBy { it.reason ?: "Не указана" }.eachCount(),
        )
    }

    context(_: Session)
    private fun perTutor(lessons: List<LessonGroup>, payments: List<PaymentRow>, slots: List<SlotDto>, now: YearMonth) =
        TutorMapper.getAll().mapNotNull { t ->
            val id = t.id.id
            val cur = lessons.filter { it.tutor == id && it.month == now }
            val weekly = slots.count { it.tutor == id && it.weekday != null }
            if (cur.isEmpty() && weekly == 0) null
            else TutorStatDto(
                id, cur.size, lessons.count { it.tutor == id && it.month == now.minusMonths(1) },
                cur.sumOf { it.price * it.students.size },
                payments.filter { it.tutor == id && it.month == now }.sumOf { it.amount.toLong() },
                weekly,
            )
        }

    /** What the slots from tomorrow to the end of the month would bill if every lesson happened. */
    private fun scheduledRest(slots: List<SlotDto>, now: YearMonth): Double =
        generateSequence(LocalDate.now().plusDays(1)) { it.plusDays(1) }
            .takeWhile { YearMonth.from(it) == now }
            .sumOf { day -> WebRepo.slotsOn(day, slots).sumOf { it.amount.toDouble() * it.students.size } }
}
