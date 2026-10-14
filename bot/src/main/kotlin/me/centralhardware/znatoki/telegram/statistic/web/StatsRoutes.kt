package me.centralhardware.znatoki.telegram.statistic.web

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

fun Route.statsRoutes() {
    /**
     * Totals behind "Итоги": per-month aggregates for the chosen period plus the period before it
     * (for the deltas), and for admins and the director the per-tutor table, a forecast for the
     * rest of the month and where students come from and why they leave.
     */
    apiGet("/stats") { c ->
        val n = if (c.seesAll) (call.request.queryParameters["months"]?.toIntOrNull() ?: 6).coerceIn(1, 12) else 1
        val scope = if (c.seesAll) null else c.id
        val now = YearMonth.now()
        val from = now.minusMonths((if (c.seesAll) 2L * n else 1L) - 1)
        val lessons = WebRepo.lessons(from.start(), now.end(), scope).filterNot { it.deleted }
        val payments = WebRepo.payments(from.start(), now.end(), scope).filterNot { it.deleted }
        val names = WebRepo.studentNames()

        val months = generateSequence(from) { it.plusMonths(1) }.takeWhile { !it.isAfter(now) }.map { m ->
            val ls = lessons.filter { it.month == m }
            MonthAggDto(
                m = m.toString(),
                lessons = ls.size,
                billed = ls.sumOf { it.price * it.students.size },
                received = payments.filter { it.month == m }.sumOf { it.amount.toLong() },
                ind = ls.count { !it.isGroup },
                grp = ls.count { it.isGroup },
                subj = ls.groupingBy { it.subject.toString() }.eachCount(),
                students = ls.flatMap { it.students }.distinct().size,
                partial = m == now,
            )
        }.toList()

        val period = lessons.filter { !it.month.isBefore(now.minusMonths(n - 1L)) }
        val weekday = (1..7).map { d -> period.count { it.dateTime.dayOfWeek.value == d } }

        val current = lessons.filter { it.month == now }
        val top = current.flatMap { it.students }.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.take(4)
            .map { CountDto(it.key, names[it.key]?.short ?: "#${it.key}", it.value) }

        val debts = if (c.seesAll) {
            val billed = mutableMapOf<Int, Double>()
            current.forEach { l -> l.students.forEach { billed[it] = (billed[it] ?: 0.0) + l.price } }
            val paid = payments.filter { it.month == now }.groupBy { it.student }.mapValues { e -> e.value.sumOf { it.amount.toLong() } }
            billed.map { (id, b) -> DebtDto(id, names[id]?.short ?: "#$id", b - (paid[id] ?: 0)) }
                .filter { it.owe > 0 }.sortedByDescending { it.owe }.take(5)
        } else null

        val admin = if (c.seesAll) adminStats(lessons, payments, now) else null
        call.respond(StatsDto(months, weekday, top, debts, admin))
    }
}

private fun adminStats(lessons: List<LessonGroup>, payments: List<PaymentRow>, now: YearMonth): StatsAdminDto {
    val names = WebRepo.studentNames()
    val slots = WebRepo.slots(null, names)
    val prev = now.minusMonths(1)
    val perTutor = TutorMapper.getAll().mapNotNull { t ->
        val id = t.id.id
        val cur = lessons.filter { it.tutor == id && it.month == now }
        val weekly = slots.count { it.tutor == id && it.weekday != null }
        if (cur.isEmpty() && weekly == 0) null
        else TutorStatDto(
            id, cur.size, lessons.count { it.tutor == id && it.month == prev },
            cur.sumOf { it.price * it.students.size },
            payments.filter { it.tutor == id && it.month == now }.sumOf { it.amount.toLong() },
            weekly,
        )
    }

    // What is billed already plus the schedule's remaining slots this month, less the usual 7–18% of skipped lessons.
    val done = lessons.filter { it.month == now }.sumOf { it.price * it.students.size }
    var rest = 0.0
    var day = LocalDate.now().plusDays(1)
    while (YearMonth.from(day) == now) {
        WebRepo.slotsOn(day, slots).forEach { rest += it.amount.toDouble() * it.students.size }
        day = day.plusDays(1)
    }

    val students = WebRepo.studentRows(includeDeleted = false, withBalance = false)
    val left = students.filter { it.status == "left" }
    val sources = students.filter { !it.source.isNullOrBlank() }.groupBy { it.source!!.trim() }
        .map { (src, list) -> SourceStatDto(src, list.size, list.count { it.status != "left" }) }
        .sortedByDescending { it.n }

    return StatsAdminDto(
        perTutor = perTutor,
        forecastDone = done,
        forecastLo = ((done + rest * 0.82) / 1000).roundToLong(),
        forecastHi = ((done + rest * 0.93) / 1000).roundToLong(),
        active = students.count { it.status == "active" },
        paused = students.count { it.status == "paused" },
        left = left.size,
        reasons = left.groupingBy { it.reason ?: "Не указана" }.eachCount(),
        sources = sources,
    )
}
