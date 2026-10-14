package me.centralhardware.znatoki.telegram.statistic.web

import io.ktor.http.ContentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.print
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport
import me.centralhardware.znatoki.telegram.statistic.report.excel
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val MONTHS_NOM = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
fun monthTitle(m: YearMonth) = "${MONTHS_NOM[m.monthValue - 1]} ${m.year}"

private val XLSX = ContentType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet")
private val DMY = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val DT = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm")
private val STATUS_TITLE = mapOf("submitted" to "на проверке", "accepted" to "принят", "returned" to "возвращён")

/** Whose report the caller may open: their own, or anyone's for admins and the director. */
private fun Caller.reportTutor(param: String?): Long =
    if (seesAll) param?.toLongOrNull() ?: id else id

private fun Caller.reportMonth(param: String?): YearMonth =
    if (seesAll) parseMonth(param) ?: YearMonth.now() else YearMonth.now()

/** The figures of the monthly .xlsx (MonthReport), per subject, as the report tab lists them. */
fun buildReport(tutor: Long, month: YearMonth, names: Map<Int, StudentName>): List<ReportSubjectDto> {
    val lessons = WebRepo.lessons(month.start(), month.end(), tutor).filterNot { it.deleted }
    val payments = WebRepo.payments(month.start(), month.end(), tutor).filterNot { it.deleted }
    return lessons.groupBy { it.subject }.map { (subject, ls) ->
        val per = linkedMapOf<Int, IntArray>()
        val sums = mutableMapOf<Int, Double>()
        ls.forEach { l ->
            l.students.forEach { s ->
                val r = per.getOrPut(s) { IntArray(2) }
                if (l.isGroup) r[1]++ else r[0]++
                sums[s] = (sums[s] ?: 0.0) + l.price
            }
        }
        val subjectPayments = payments.filter { it.subject == subject }
        val students = per.keys.map { id ->
            ReportStudentDto(
                id, names[id]?.fio ?: "#$id", names[id]?.deleted ?: false, per[id]!![0], per[id]!![1], sums[id] ?: 0.0,
                subjectPayments.filter { it.student == id && it.photo != null }.sumOf { it.amount.toLong() },
            )
        }.sortedBy { it.fio }
        ReportSubjectDto(subject, students, ls.size, sums.values.sum(), subjectPayments.sumOf { it.amount.toLong() })
    }.sortedBy { it.subject }
}

fun Route.reportRoutes(ctx: WebContext) {
    apiGet("/report") { c ->
        val tutor = c.reportTutor(call.request.queryParameters["tutor"])
        val month = c.reportMonth(call.request.queryParameters["month"])
        val names = WebRepo.studentNames()
        val payroll = if (c.isAdmin) TutorMapper.getAll().mapNotNull { t ->
            val billed = WebRepo.lessons(month.start(), month.end(), t.id.id).filterNot { it.deleted }.sumOf { it.price * it.students.size }
            if (billed == 0.0) null else PayrollRowDto(t.id.id, t.rate, billed, t.rate?.let { Math.round(billed * it / 100.0) })
        } else null
        call.respond(ReportDto(tutor, month.toString(), buildReport(tutor, month, names), WebRepo.submission(tutor, month.toString()), payroll))
    }

    /** The three sheets of the .xlsx for one subject, so the app can show them without a download. */
    apiGet("/report/preview") { c ->
        val tutorId = c.reportTutor(call.request.queryParameters["tutor"])
        val month = c.reportMonth(call.request.queryParameters["month"])
        val subject = call.request.queryParameters["subject"]?.toLongOrNull() ?: badRequest("Нет предмета.")
        val tutor = TutorMapper.findByIdOrNull(TutorId(tutorId)) ?: notFound()
        val names = WebRepo.studentNames()
        val subjects = subjectNames()
        val lessons = WebRepo.lessons(month.start(), month.end(), tutorId).filter { !it.deleted && it.subject == subject }.sortedBy { it.dateTime }
        val pays = WebRepo.payments(month.start(), month.end(), tutorId).filter { !it.deleted && it.subject == subject }.sortedBy { it.dateTime }
        val showPay = c.readsPayments

        val perStudent = linkedMapOf<Int, MutableList<LessonGroup>>()
        lessons.forEach { l -> l.students.forEach { perStudent.getOrPut(it) { mutableListOf() }.add(l) } }
        val order = perStudent.keys.sortedWith(compareBy<Int> { names[it]?.cls ?: Int.MAX_VALUE }.thenBy { names[it]?.fio ?: "" })
        var ti = 0L; var tg = 0L
        val att = order.mapIndexed { i, id ->
            val ls = perStudent[id]!!
            val ind = ls.count { !it.isGroup }; val grp = ls.count { it.isGroup }
            ti += ind; tg += grp
            val dates = ls.groupingBy { it.dateTime.format(DMY) }.eachCount().entries.joinToString(",") { "${it.key}(${it.value})" }
            AttRowDto(i + 1, names[id]?.fio ?: "#$id", names[id]?.cls, ind, grp,
                if (showPay) pays.filter { it.student == id && it.photo != null }.sumOf { it.amount.toLong() } else null, dates)
        }
        val log = lessons.map { l ->
            LogRowDto(
                l.dateTime.format(DT),
                if (l.students.size == 1) names[l.students[0]]?.fio ?: "" else l.students.mapIndexed { i, s -> "${i + 1} - ${names[s]?.fio ?: s}" }.joinToString("\n"),
                l.price,
                if (l.students.size == 1) l.forceGroup.print() else "",
                l.half.print(),
            )
        }
        call.respond(
            PreviewDto(
                name = "${tutor.name} - ${subjects[subject]} ${monthTitle(month)}.xlsx",
                title = subjects[subject] ?: "",
                tutorName = tutor.name,
                monthTitle = monthTitle(month).lowercase(),
                att = att,
                attTotals = listOf(ti, tg, if (showPay) pays.sumOf { it.amount.toLong() } else null),
                log = log,
                logTotal = lessons.sumOf { it.price },
                pays = if (showPay) pays.map { PayRowDto(it.dateTime.format(DT), names[it.student]?.fio ?: "", it.amount) } else null,
                payTotal = if (showPay) pays.sumOf { it.amount.toLong() } else null,
            )
        )
    }

    post("/report/export") {
        val c = call.caller
        val input = call.receive<ExportInput>()
        val month = c.reportMonth(input.month)
        val files: List<File> = withContext(Dispatchers.IO) {
            when (input.kind) {
                "month" -> {
                    val tutors = if (c.isAdmin && input.tutor == "all") TutorMapper.getAll() else listOf(c.tutor).let { own ->
                        if (c.seesAll && input.tutor.toLongOrNull() != null) listOfNotNull(TutorMapper.findByIdOrNull(TutorId(input.tutor.toLong()))) else own
                    }
                    tutors.flatMap { monthFiles(it, month, input.subjects) }
                }
                "payments" -> {
                    if (!c.readsPayments) forbidden("Нет доступа к оплатам.")
                    listOf(paymentsFile(c, month))
                }
                "payroll" -> {
                    if (!c.isAdmin) forbidden()
                    listOf(payrollFile(month))
                }
                "audit" -> {
                    if (!c.isAdmin) forbidden()
                    listOf(auditCsv(input.filter))
                }
                else -> badRequest("Неизвестный тип выгрузки.")
            }
        }
        if (files.isEmpty()) badRequest("За ${monthTitle(month).lowercase()} по выбранным предметам занятий нет.")
        if (input.to == "chat") {
            files.forEach { ctx.notifier.sendFile(c.id, it); it.delete() }
            call.respond(SentDto(files.size))
        } else {
            val (file, type) = if (files.size == 1) files[0] to (if (files[0].name.endsWith(".csv")) ContentType.Text.CSV else XLSX)
            else zip(files, "Отчёты ${monthTitle(month)}.zip") to ContentType.Application.Zip
            call.respond(DownloadDto(Downloads.register(file, file.name, type), file.name, files.size))
        }
    }

    /** Director's inbox: every teaching tutor's month with the submission status. */
    apiGet("/report/inbox") { c ->
        if (!c.seesAll) forbidden()
        val month = c.reportMonth(call.request.queryParameters["month"])
        val subs = WebRepo.submissions(month.toString()).associateBy { it.tutor }
        val rows = TutorMapper.getAll()
            .filter { it.subjects.isNotEmpty() || Permissions.ADD_TIME in it.permissions || it.id.id in subs }
            .map { InboxRowDto(it.id.id, WebRepo.monthTotals(it.id.id, month), subs[it.id.id]) }
        call.respond(rows)
    }

    post("/report/submit") {
        val c = call.caller
        if (!c.canTime) forbidden()
        val input = call.receive<SubmitInput>()
        val month = parseMonth(input.month) ?: badRequest("Неверный месяц.")
        val prev = WebRepo.submission(c.id, month.toString())
        if (prev != null && prev.status != "returned") badRequest("Отчёт уже сдан.")
        val s = SubmissionDto(c.id, month.toString(), "submitted", input.note?.trim()?.takeIf { it.isNotEmpty() }, null, LocalDateTime.now().toString())
        WebRepo.saveSubmission(s, null)
        AuditService.log(c.id, "Отчёт сдан", "report", "${c.id}-$month", "${c.tutor.name} · ${monthTitle(month)}",
            listOf(Change("статус", if (prev == null) "черновик" else STATUS_TITLE[prev.status], "на проверке")), DataSource.WEB)
        ctx.notifier.toPermission(
            Permissions.DIRECTOR,
            "📄 ${c.tutor.name} сдал(а) отчёт за ${monthTitle(month).lowercase()}." + (s.note?.let { "\nКомментарий: $it" } ?: ""),
            "Открыть отчёты" to "/#tab=report&month=$month",
        )
        call.respond(s)
    }

    post("/report/{tutor}/{month}/{action}") {
        val c = call.caller
        if (!c.seesAll) forbidden("Проверять отчёты может директор.")
        val tutorId = call.parameters["tutor"]?.toLongOrNull() ?: notFound()
        val month = parseMonth(call.parameters["month"]) ?: notFound()
        val tutor = TutorMapper.findByIdOrNull(TutorId(tutorId)) ?: notFound()
        val prev = WebRepo.submission(tutorId, month.toString())
        val now = LocalDateTime.now().toString()
        val title = "${tutor.name} · ${monthTitle(month)}"
        val oldStatus = prev?.status?.let { STATUS_TITLE[it] } ?: "не сдан"
        val next = when (call.parameters["action"]) {
            "accept" -> {
                if (prev?.status == "accepted") badRequest("Отчёт уже принят.")
                AuditService.log(c.id, "Отчёт принят", "report", "$tutorId-$month", title, listOf(Change("статус", oldStatus, "принят")), DataSource.WEB)
                ctx.notifier.send(tutorId, "✅ Отчёт за ${monthTitle(month).lowercase()} принят.", "Открыть отчёт" to "/#tab=report")
                (prev ?: SubmissionDto(tutorId, month.toString(), "accepted")).copy(status = "accepted", decidedAt = now, selfAccepted = prev?.status != "submitted")
            }
            "return" -> {
                val comment = call.receive<ReturnInput>().comment.trim()
                if (comment.isEmpty()) badRequest("Напишите, что исправить: репетитор увидит этот текст.")
                AuditService.log(c.id, "Отчёт возвращён", "report", "$tutorId-$month", title,
                    listOf(Change("статус", oldStatus, "возвращён"), Change("комментарий", null, comment)), DataSource.WEB)
                ctx.notifier.send(tutorId, "↩️ Отчёт за ${monthTitle(month).lowercase()} возвращён на доработку:\n«$comment»", "Открыть отчёт" to "/#tab=report")
                (prev ?: SubmissionDto(tutorId, month.toString(), "returned")).copy(status = "returned", comment = comment, decidedAt = now)
            }
            "reopen" -> {
                if (prev?.status != "accepted") badRequest("Отчёт не принят.")
                AuditService.log(c.id, "Отметка снята", "report", "$tutorId-$month", title, listOf(Change("статус", "принят", "на проверке")), DataSource.WEB)
                prev.copy(status = "submitted", decidedAt = null, selfAccepted = false)
            }
            else -> notFound()
        }
        WebRepo.saveSubmission(next, c.id)
        call.respond(next)
    }
}

private fun monthFiles(tutor: Tutor, month: YearMonth, subjectFilter: List<Long>): List<File> {
    val subjects = (tutor.subjects.map { it.id } + WebRepo.lessons(month.start(), month.end(), tutor.id.id).map { it.subject }).distinct()
        .filter { subjectFilter.isEmpty() || it in subjectFilter }
    return subjects.mapNotNull { s ->
        val lessons = LessonMapper.getTimesByMonth(tutor.id, SubjectId(s), month)
        if (lessons.isEmpty()) null
        else MonthReport(tutor.name, SubjectId(s), tutor.id).generate(lessons, PaymentMapper.getPaymentsByMonth(tutor.id, SubjectId(s), month))
    }
}

private fun paymentsFile(c: Caller, month: YearMonth): File {
    val names = WebRepo.studentNames()
    val subjects = subjectNames()
    val tutors = TutorMapper.getAll().associate { it.id.id to it.name }
    val list = WebRepo.payments(month.start(), month.end(), if (c.seesAll) null else c.id).filterNot { it.deleted }.sortedBy { it.dateTime }
    return excel {
        sheet("Оплаты") {
            title("Оплаты · ${monthTitle(month).lowercase()}", 4)
            row { cell("Дата"); cell("Ученик"); cell("Предмет"); cell("Принял"); cell("Сумма") }
            list.forEach { p ->
                row {
                    cell(p.dateTime.format(DT)); cell(names[p.student]?.fio ?: ""); cell(subjects[p.subject] ?: "")
                    cell(tutors[p.tutor] ?: ""); cell(p.amount)
                }
            }
            row { cell("Итого"); emptyCell(); emptyCell(); emptyCell(); cell(list.sumOf { it.amount }) }
        }
    }.build("Оплаты ${monthTitle(month)}.xlsx")
}

private fun payrollFile(month: YearMonth): File {
    val rows = TutorMapper.getAll().mapNotNull { t ->
        val billed = WebRepo.lessons(month.start(), month.end(), t.id.id).filterNot { it.deleted }.sumOf { it.price * it.students.size }
        if (billed == 0.0) null else Triple(t, billed, t.rate?.let { Math.round(billed * it / 100.0) })
    }
    return excel {
        sheet("Ведомость") {
            title("Зарплата репетиторов · ${monthTitle(month).lowercase()}", 3)
            row { cell("Преподаватель"); cell("Начислено"); cell("Ставка, %"); cell("К выплате") }
            rows.forEach { (t, billed, pay) -> row { cell(t.name); cell(billed); cell(t.rate ?: "не задана"); cell(pay ?: "") } }
            row { cell("Итого"); cell(rows.sumOf { it.second }); emptyCell(); cell(rows.sumOf { it.third ?: 0 }) }
        }
    }.build("Ведомость ${monthTitle(month)}.xlsx")
}

private fun zip(files: List<File>, name: String): File {
    val out = Files.createTempDirectory("classlog").resolve(name).toFile()
    ZipOutputStream(out.outputStream()).use { zos ->
        files.forEach { f ->
            zos.putNextEntry(ZipEntry(f.name))
            f.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
            f.delete()
        }
    }
    return out
}
