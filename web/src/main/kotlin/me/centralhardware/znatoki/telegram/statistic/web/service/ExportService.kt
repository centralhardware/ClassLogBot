package me.centralhardware.znatoki.telegram.statistic.web.service

import kotliquery.Session
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport
import me.centralhardware.znatoki.telegram.statistic.report.excel
import me.centralhardware.znatoki.telegram.statistic.web.*
import me.centralhardware.znatoki.telegram.statistic.web.http.badRequest
import me.centralhardware.znatoki.telegram.statistic.web.http.forbidden
import java.io.File
import java.nio.file.Files
import java.time.YearMonth
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val XLSX = ContentType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet")

/**
 * "Выгрузить": builds the files (the same .xlsx as /report, a payments list, the payroll or the
 * audit CSV) and either sends them to the caller's chat with the bot or hands back a download link.
 */
object ExportService {

    /** "В чат": the bot sends the files to the caller, as /report does. */
    context(_: Session)
    suspend fun toChat(c: Caller, input: ExportInput, month: YearMonth): SentDto {
        val files = files(c, input, month)
        files.forEach { Notifications.sendFile(c.id, it); it.delete() }
        return SentDto(files.size)
    }

    /** "Скачать": one file as is, several in a .zip, behind a short-lived download link. */
    context(_: Session)
    suspend fun toDownload(c: Caller, input: ExportInput, month: YearMonth): DownloadDto {
        val files = files(c, input, month)
        val (file, type) = when {
            files.size > 1 -> zip(files, "Отчёты ${monthTitle(month)}.zip") to ContentType.Application.Zip
            files[0].name.endsWith(".csv") -> files[0] to ContentType.Text.CSV
            else -> files[0] to XLSX
        }
        return DownloadDto(Downloads.register(file, file.name, type), file.name, files.size)
    }

    context(_: Session)
    private suspend fun files(c: Caller, input: ExportInput, month: YearMonth): List<File> =
        withContext(Dispatchers.IO) { build(c, input, month) }
            .ifEmpty { badRequest("За ${monthTitle(month).lowercase()} по выбранным предметам занятий нет.") }

    context(_: Session)
    private fun build(c: Caller, input: ExportInput, month: YearMonth): List<File> = when (input.kind) {
        "month" -> tutorsFor(c, input.tutor).flatMap { monthFiles(it, month, input.subjects) }
        "payments" -> { if (!c.readsPayments) forbidden("Нет доступа к оплатам."); listOf(paymentsFile(c, month)) }
        "payroll" -> { if (!c.isAdmin) forbidden(); listOf(payrollFile(month)) }
        "audit" -> { if (!c.isAdmin) forbidden(); listOf(AuditLogService.csv(AuditFilter(type = input.filter))) }
        else -> badRequest("Неизвестный тип выгрузки.")
    }

    /** "me", "all" (admins) or one tutor's id (admins and the director). */
    context(_: Session)
    private fun tutorsFor(c: Caller, who: String): List<Tutor> = when {
        who == "all" && c.seesAll -> TutorMapper.getAll()
        who.toLongOrNull() != null && c.seesAll -> listOfNotNull(TutorMapper.findByIdOrNull(TutorId(who.toLong())))
        else -> listOf(c.tutor)
    }

    /** One MonthReport file per subject the tutor had lessons in. */
    context(_: Session)
    private fun monthFiles(tutor: Tutor, month: YearMonth, subjectFilter: List<Long>): List<File> =
        (tutor.subjects.map { it.id } + WebRepo.lessons(month.start(), month.end(), tutor.id.id).map { it.subject })
            .distinct()
            .filter { subjectFilter.isEmpty() || it in subjectFilter }
            .mapNotNull { s ->
                val lessons = LessonMapper.getTimesByMonth(tutor.id, SubjectId(s), month)
                if (lessons.isEmpty()) null
                else MonthReport(tutor.name, SubjectId(s), tutor.id).generate(lessons, PaymentMapper.getPaymentsByMonth(tutor.id, SubjectId(s), month))
            }

    context(_: Session)
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
                    row { cell(p.dateTime.format(DT)); cell(names[p.student]?.fio ?: ""); cell(subjects[p.subject] ?: ""); cell(tutors[p.tutor] ?: ""); cell(p.amount) }
                }
                row { cell("Итого"); emptyCell(); emptyCell(); emptyCell(); cell(list.sumOf { it.amount }) }
            }
        }.build("Оплаты ${monthTitle(month)}.xlsx")
    }

    context(_: Session)
    private fun payrollFile(month: YearMonth): File {
        val rows = ReportWebService.payroll(month)
        return excel {
            sheet("Ведомость") {
                title("Зарплата репетиторов · ${monthTitle(month).lowercase()}", 3)
                row { cell("Преподаватель"); cell("Начислено"); cell("Ставка, %"); cell("К выплате") }
                rows.forEach { r ->
                    row { cell(TutorMapper.findByIdOrNull(TutorId(r.tutor))?.name ?: r.tutor); cell(r.billed); cell(r.rate ?: "не задана"); cell(r.pay ?: "") }
                }
                row { cell("Итого"); cell(rows.sumOf { it.billed }); emptyCell(); cell(rows.sumOf { it.pay ?: 0 }) }
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
}
