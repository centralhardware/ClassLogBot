package me.centralhardware.znatoki.telegram.statistic.app

// Mirror of the server's WebDto.kt (bot module); keep the two in sync.

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ErrorDto(val error: String, val code: String? = null)

@Serializable
data class AuthRequest(val initData: String = "", val platform: String? = null, val login: String? = null) {
    /** initData inside Telegram; outside it the stored sign-in (`tgb …`, or bare Login Widget data from older clients). */
    val credential get() = login?.let { if (it.startsWith("tgb ") || it.startsWith("tlw ")) it else "tlw $it" } ?: "tma $initData"
}

/** A pending sign-in through the bot: open [link] in Telegram, then poll with [token]. */
@Serializable
data class BotLoginDto(val token: String, val link: String)

/** The credential once the bot confirmed the sign-in, null while still waiting. */
@Serializable
data class BotLoginPollDto(val credential: String? = null)

@Serializable
data class MeDto(val id: Long, val name: String, val perms: List<String>, val subjects: List<Long>)

@Serializable
data class CapsDto(
    val time: Boolean,
    val payment: Boolean,
    val readPayments: Boolean,
    val client: Boolean,
    val admin: Boolean,
    val director: Boolean,
    val manageTutors: Boolean,
    val forceGroup: Boolean,
    val extraHalfHour: Boolean,
    val timeForOthers: Boolean,
    val paymentForOthers: Boolean,
    val seesAll: Boolean,
)

@Serializable
data class SubjectDto(val id: Long, val name: String, val group: Boolean)

@Serializable
data class TutorBriefDto(val id: Long, val name: String, val subjects: List<Long>, val teaching: Boolean, val active: Boolean)

@Serializable
data class BootDto(
    val me: MeDto,
    val caps: CapsDto,
    val today: String,
    val months: List<String>,
    val subjects: List<SubjectDto>,
    val tutors: List<TutorBriefDto>,
    val sources: List<String>,
    val reasons: List<String>,
)

@Serializable
data class StudentRefDto(val id: Int, val fio: String, val cls: Int? = null, val deleted: Boolean = false)

@Serializable
data class LessonDto(
    val id: String,
    val date: String,
    val time: String,
    val subject: Long,
    val students: List<StudentRefDto>,
    val amount: Int,
    val half: Boolean,
    val forceGroup: Boolean,
    val deleted: Boolean,
    val tutor: Long,
    val addedBy: Long? = null,
    val source: String? = null,
    val hasPhoto: Boolean,
    val flags: List<String> = emptyList(),
)

@Serializable
data class PaymentDto(
    val id: Int,
    val date: String,
    val time: String,
    val student: StudentRefDto,
    val subject: Long,
    val amount: Int,
    val tutor: Long,
    val deleted: Boolean,
    val hasPhoto: Boolean,
    val addedBy: Long? = null,
    val source: String? = null,
)

@Serializable
data class StudentRowDto(
    val id: Int,
    val last: String,
    val first: String,
    val middle: String,
    val cls: Int?,
    val status: String,
    val reason: String?,
    val since: String?,
    val deleted: Boolean,
    val monthCount: Int,
    val subjects: List<Long>,
    val lastDate: String?,
    val balance: Long? = null,
    val source: String? = null,
)

@Serializable
data class NoteDto(val at: String, val by: String, val text: String)

@Serializable
data class RecentLessonDto(val date: String, val subject: Long, val group: Boolean)

@Serializable
data class StudentDetailDto(
    val row: StudentRowDto,
    val phone: String?,
    val parent: String?,
    val motherFio: String?,
    val birthDate: String?,
    val recordDate: String?,
    val notes: List<NoteDto>,
    val lessons: Int,
    val paid: Long?,
    val recent: List<RecentLessonDto>,
)

@Serializable
data class StudentInput(
    val last: String,
    val first: String,
    val middle: String = "",
    val cls: Int? = null,
    val phone: String? = null,
    val parent: String? = null,
    val source: String? = null,
)

@Serializable
data class StatusInput(val status: String, val reason: String? = null)

@Serializable
data class TextInput(val text: String)

@Serializable
data class LessonPatch(
    val half: Boolean? = null,
    val forceGroup: Boolean? = null,
    val deleted: Boolean? = null,
    val amount: Int? = null,
    val subject: Long? = null,
    val tutor: Long? = null,
    val date: String? = null,
    val time: String? = null,
)

@Serializable
data class BulkInput(val ids: List<String>, val action: String, val value: String? = null)

@Serializable
data class PaymentPatch(val amount: Int? = null, val subject: Long? = null, val tutor: Long? = null, val deleted: Boolean? = null)

@Serializable
data class SlotDto(
    val id: Int,
    val weekday: Int?,
    val onceDate: String?,
    val time: String,
    val subject: Long,
    val students: List<StudentRefDto>,
    val amount: Int,
    val tutor: Long,
)

@Serializable
data class SlotMarkDto(val slot: Int, val date: String, val status: String, val lesson: String? = null)

@Serializable
data class RemindersDto(val evening: Boolean, val at: String, val monthEnd: Boolean)

@Serializable
data class ScheduleDto(val slots: List<SlotDto>, val marks: List<SlotMarkDto>, val reminders: RemindersDto)

@Serializable
data class SlotInput(
    val weekday: Int? = null,
    val date: String? = null,
    val time: String,
    val subject: Long,
    val students: List<Int>,
    val amount: Int,
)

@Serializable
data class DateInput(val date: String)

@Serializable
data class SubmissionDto(
    val tutor: Long,
    val month: String,
    val status: String,
    val note: String? = null,
    val comment: String? = null,
    val submittedAt: String? = null,
    val decidedAt: String? = null,
    val selfAccepted: Boolean = false,
)

@Serializable
data class SubmitInput(val month: String, val note: String? = null)

@Serializable
data class ReturnInput(val comment: String)

@Serializable
data class MonthTotalsDto(val lessons: Int, val billed: Long, val received: Long, val subjects: List<Long>)

@Serializable
data class InboxRowDto(val tutor: Long, val totals: MonthTotalsDto, val submission: SubmissionDto? = null)

@Serializable
data class ReportStudentDto(val id: Int, val fio: String, val deleted: Boolean, val ind: Int, val grp: Int, val sum: Double, val paid: Long)

@Serializable
data class ReportSubjectDto(val subject: Long, val students: List<ReportStudentDto>, val lessons: Int, val billed: Double, val received: Long)

@Serializable
data class PayrollRowDto(val tutor: Long, val rate: Int?, val billed: Double, val pay: Long?)

@Serializable
data class ReportDto(
    val tutor: Long,
    val month: String,
    val subjects: List<ReportSubjectDto>,
    val submission: SubmissionDto? = null,
    val payroll: List<PayrollRowDto>? = null,
)

@Serializable
data class AttRowDto(val n: Int, val fio: String, val cls: Int?, val ind: Int, val grp: Int, val paid: Long?, val dates: String)

@Serializable
data class LogRowDto(val at: String, val fio: String, val price: Double, val group: String, val half: String)

@Serializable
data class PayRowDto(val at: String, val fio: String, val amount: Int)

@Serializable
data class PreviewDto(
    val name: String,
    val title: String,
    val tutorName: String,
    val monthTitle: String,
    val att: List<AttRowDto>,
    val attTotals: List<Long?>,
    val log: List<LogRowDto>,
    val logTotal: Double,
    val pays: List<PayRowDto>?,
    val payTotal: Long?,
)

@Serializable
data class ExportInput(
    val kind: String,
    val month: String,
    val subjects: List<Long> = emptyList(),
    val tutor: String = "me",
    val to: String = "file",
    /** Audit export only: the entity type the journal is filtered to. */
    val filter: String? = null,
)

@Serializable
data class DownloadDto(val url: String, val name: String, val count: Int = 1)

@Serializable
data class SentDto(val sent: Int)

@Serializable
data class MonthAggDto(
    val m: String,
    val lessons: Int,
    val billed: Double,
    val received: Long,
    val ind: Int,
    val grp: Int,
    val subj: Map<String, Int>,
    val students: Int,
    val partial: Boolean = false,
)

@Serializable
data class CountDto(val id: Int, val fio: String, val n: Int)

@Serializable
data class DebtDto(val id: Int, val fio: String, val owe: Double)

@Serializable
data class TutorStatDto(val tutor: Long, val n: Int, val np: Int, val billed: Double, val received: Long, val slots: Int)

@Serializable
data class StatsAdminDto(
    val perTutor: List<TutorStatDto>,
    val forecastDone: Double,
    val forecastLo: Long,
    val forecastHi: Long,
    val active: Int,
    val paused: Int,
    val left: Int,
    val reasons: Map<String, Int>,
)

@Serializable
data class StatsDto(
    val months: List<MonthAggDto>,
    val weekday: List<Int>,
    val top: List<CountDto>,
    val debts: List<DebtDto>?,
    val admin: StatsAdminDto? = null,
)

@Serializable
data class TutorDto(
    val id: Long,
    val name: String,
    val perms: List<String>,
    val subjects: List<Long>,
    val rate: Int?,
)

@Serializable
data class TutorInput(val id: Long, val name: String, val perms: List<String>, val subjects: List<Long>, val rate: Int? = null)

@Serializable
data class AuditDto(
    val n: Int,
    val at: String,
    val actor: Long,
    val actorName: String,
    val source: String,
    val action: String,
    val type: String,
    val id: String,
    val title: String,
    val changes: JsonElement? = null,
)

@Serializable
data class AuditPageDto(val total: Int, val items: List<AuditDto>)

@Serializable
data class AlertsDto(val mass: Boolean = true, val night: Boolean = true, val closed: Boolean = true)

@Serializable
data class SearchDto(val students: List<StudentRowDto>, val lessons: List<LessonDto>, val payments: List<PaymentDto>)

@Serializable
data class OkDto(val ok: Boolean = true, val id: String? = null)

@Serializable
data class LessonInput(
    val tutor: Long? = null,
    val subject: Long,
    val students: List<Int>,
    val amount: Int,
    val date: String? = null,
    val time: String? = null,
    val slot: Int? = null,
)

@Serializable
data class PaymentInput(val tutor: Long? = null, val student: Int, val subject: Long, val amount: Int)

@Serializable
data class LastPriceDto(val amount: String? = null, val date: String? = null)
