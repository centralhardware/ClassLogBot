package me.centralhardware.znatoki.telegram.statistic.web.service

import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.print
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.web.*
import me.centralhardware.znatoki.telegram.statistic.web.http.badRequest
import me.centralhardware.znatoki.telegram.statistic.web.http.notFound
import java.time.LocalDateTime
import java.time.YearMonth

/** The monthly report tab: the figures of the .xlsx, its preview, and payroll for admins. */
object ReportWebService {

    fun report(c: Caller, tutor: Long, month: YearMonth): ReportDto =
        ReportDto(tutor, month.toString(), subjects(tutor, month), WebRepo.submission(tutor, month.toString()), if (c.isAdmin) payroll(month) else null)

    /** Per subject and student: individual and group lessons, what was billed and what was paid (with a receipt), as in MonthReport. */
    private fun subjects(tutor: Long, month: YearMonth): List<ReportSubjectDto> {
        val names = WebRepo.studentNames()
        val lessons = WebRepo.lessons(month.start(), month.end(), tutor).filterNot { it.deleted }
        val payments = WebRepo.payments(month.start(), month.end(), tutor).filterNot { it.deleted }
        return lessons.groupBy { it.subject }.map { (subject, ls) ->
            val paid = payments.filter { it.subject == subject }
            val students = ls.flatMap { it.students }.distinct().map { id ->
                val own = ls.filter { id in it.students }
                ReportStudentDto(
                    id, names[id]?.fio ?: "#$id", names[id]?.deleted ?: false,
                    own.count { !it.isGroup }, own.count { it.isGroup }, own.sumOf { it.price },
                    paid.filter { it.student == id && it.photo != null }.sumOf { it.amount.toLong() },
                )
            }.sortedBy { it.fio }
            ReportSubjectDto(subject, students, ls.size, students.sumOf { it.sum }, paid.sumOf { it.amount.toLong() })
        }.sortedBy { it.subject }
    }

    /** Each tutor's share of what they billed this month; tutors without a rate are listed without a sum. */
    fun payroll(month: YearMonth): List<PayrollRowDto> = TutorMapper.getAll().mapNotNull { t ->
        val billed = WebRepo.lessons(month.start(), month.end(), t.id.id).filterNot { it.deleted }.sumOf { it.price * it.students.size }
        if (billed == 0.0) null else PayrollRowDto(t.id.id, t.rate, billed, t.rate?.let { Math.round(billed * it / 100.0) })
    }

    /** The three sheets of the .xlsx for one subject, so the app can show them without a download. */
    fun preview(c: Caller, tutorId: Long, month: YearMonth, subject: Long): PreviewDto {
        val tutor = TutorMapper.findByIdOrNull(TutorId(tutorId)) ?: notFound()
        val names = WebRepo.studentNames()
        val lessons = WebRepo.lessons(month.start(), month.end(), tutorId).filter { !it.deleted && it.subject == subject }.sortedBy { it.dateTime }
        val pays = if (c.readsPayments) WebRepo.payments(month.start(), month.end(), tutorId).filter { !it.deleted && it.subject == subject }.sortedBy { it.dateTime } else null
        val att = attendance(lessons, pays, names)
        val subjectName = subjectNames()[subject] ?: ""
        return PreviewDto(
            name = "${tutor.name} - $subjectName ${monthTitle(month)}.xlsx",
            title = subjectName,
            tutorName = tutor.name,
            monthTitle = monthTitle(month).lowercase(),
            att = att,
            attTotals = listOf(att.sumOf { it.ind.toLong() }, att.sumOf { it.grp.toLong() }, pays?.sumOf { it.amount.toLong() }),
            log = lessons.map { l ->
                LogRowDto(
                    l.dateTime.format(DT),
                    if (l.students.size == 1) names[l.students[0]]?.fio ?: "" else l.students.mapIndexed { i, s -> "${i + 1} - ${names[s]?.fio ?: s}" }.joinToString("\n"),
                    l.price,
                    if (l.students.size == 1) l.forceGroup.print() else "",
                    l.half.print(),
                )
            },
            logTotal = lessons.sumOf { it.price },
            pays = pays?.map { PayRowDto(it.dateTime.format(DT), names[it.student]?.fio ?: "", it.amount) },
            payTotal = pays?.sumOf { it.amount.toLong() },
        )
    }

    /** The "отчет" sheet: one row per student, ordered by class and name like MonthReport. */
    private fun attendance(lessons: List<LessonGroup>, pays: List<PaymentRow>?, names: Map<Int, StudentName>): List<AttRowDto> =
        lessons.flatMap { it.students }.distinct()
            .sortedWith(compareBy<Int> { names[it]?.cls ?: Int.MAX_VALUE }.thenBy { names[it]?.fio ?: "" })
            .mapIndexed { i, id ->
                val own = lessons.filter { id in it.students }
                AttRowDto(
                    i + 1, names[id]?.fio ?: "#$id", names[id]?.cls, own.count { !it.isGroup }, own.count { it.isGroup },
                    pays?.filter { it.student == id && it.photo != null }?.sumOf { it.amount.toLong() },
                    own.groupingBy { it.dateTime.format(DMY) }.eachCount().entries.joinToString(",") { "${it.key}(${it.value})" },
                )
            }
}

/** Handing the month's report to the director, and the director's answer. */
object SubmissionService {

    private val STATUS_TITLE = mapOf("submitted" to "на проверке", "accepted" to "принят", "returned" to "возвращён")

    /** Director's inbox: every teaching tutor's month with its submission status. */
    fun inbox(month: YearMonth): List<InboxRowDto> {
        val subs = WebRepo.submissions(month.toString()).associateBy { it.tutor }
        return TutorMapper.getAll()
            .filter { (it.permissions.isNotEmpty() && (it.subjects.isNotEmpty() || Permissions.ADD_TIME in it.permissions)) || it.id.id in subs }
            .map { InboxRowDto(it.id.id, WebRepo.monthTotals(it.id.id, month), subs[it.id.id]) }
    }

    fun submit(c: Caller, input: SubmitInput): SubmissionDto {
        val month = parseMonth(input.month) ?: badRequest("Неверный месяц.")
        val prev = WebRepo.submission(c.id, month.toString())
        if (prev != null && prev.status != "returned") badRequest("Отчёт уже сдан.")
        val s = SubmissionDto(c.id, month.toString(), "submitted", input.note?.trim()?.takeIf { it.isNotEmpty() }, null, LocalDateTime.now().toString())
        WebRepo.saveSubmission(s, null)
        log(c, "Отчёт сдан", c.id, c.tutor.name, month, listOf(Change("статус", prev?.let { STATUS_TITLE[it.status] } ?: "черновик", "на проверке")))
        Notifications.sendTo(
            Permissions.DIRECTOR,
            "📄 ${c.tutor.name} сдал(а) отчёт за ${monthTitle(month).lowercase()}." + (s.note?.let { "\nКомментарий: $it" } ?: ""),
            "tab=report&month=$month",
        )
        return s
    }

    /** Accepting works for a report that was never handed in, too ("Отметить как принятый"). */
    fun accept(c: Caller, tutorId: Long, month: YearMonth): SubmissionDto {
        val (tutorName, prev) = load(tutorId, month)
        if (prev?.status == "accepted") badRequest("Отчёт уже принят.")
        log(c, "Отчёт принят", tutorId, tutorName, month, listOf(Change("статус", prev?.let { STATUS_TITLE[it.status] } ?: "не сдан", "принят")))
        Notifications.send(tutorId, "✅ Отчёт за ${monthTitle(month).lowercase()} принят.", "tab=report")
        return save(c, (prev ?: SubmissionDto(tutorId, month.toString(), "accepted"))
            .copy(status = "accepted", decidedAt = LocalDateTime.now().toString(), selfAccepted = prev?.status != "submitted"))
    }

    fun returnBack(c: Caller, tutorId: Long, month: YearMonth, comment: String): SubmissionDto {
        val (tutorName, prev) = load(tutorId, month)
        val text = comment.trim().ifEmpty { badRequest("Напишите, что исправить: репетитор увидит этот текст.") }
        log(c, "Отчёт возвращён", tutorId, tutorName, month,
            listOf(Change("статус", prev?.let { STATUS_TITLE[it.status] } ?: "не сдан", "возвращён"), Change("комментарий", null, text)))
        Notifications.send(tutorId, "↩️ Отчёт за ${monthTitle(month).lowercase()} возвращён на доработку:\n«$text»", "tab=report")
        return save(c, (prev ?: SubmissionDto(tutorId, month.toString(), "returned"))
            .copy(status = "returned", comment = text, decidedAt = LocalDateTime.now().toString()))
    }

    fun reopen(c: Caller, tutorId: Long, month: YearMonth): SubmissionDto {
        val (tutorName, prev) = load(tutorId, month)
        if (prev?.status != "accepted") badRequest("Отчёт не принят.")
        log(c, "Отметка снята", tutorId, tutorName, month, listOf(Change("статус", "принят", "на проверке")))
        return save(c, prev.copy(status = "submitted", decidedAt = null, selfAccepted = false))
    }

    private fun load(tutorId: Long, month: YearMonth): Pair<String, SubmissionDto?> =
        (TutorMapper.findByIdOrNull(TutorId(tutorId)) ?: notFound()).name to WebRepo.submission(tutorId, month.toString())

    private fun save(c: Caller, s: SubmissionDto) = s.also { WebRepo.saveSubmission(it, c.id) }

    private fun log(c: Caller, action: String, tutorId: Long, tutorName: String, month: YearMonth, changes: List<Change>) =
        AuditService.log(c.id, action, "report", "$tutorId-$month", "$tutorName · ${monthTitle(month)}", changes, DataSource.WEB)
}
