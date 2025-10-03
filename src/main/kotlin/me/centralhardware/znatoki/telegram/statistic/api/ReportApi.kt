package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.warning
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.extensions.hasReadRight
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Serializable
data class ReportDataDto(
    val tutorName: String,
    val subjectName: String,
    val month: String,
    val year: Int,
    val students: List<StudentReportDto>,
    val totalIndividual: Int,
    val totalGroup: Int,
    val totalPayments: Int,
    val lessons: List<LessonReportDto>,
    val payments: List<PaymentReportDto>
)

@Serializable
data class StudentReportDto(
    val fio: String,
    val schoolClass: String,
    val individual: Int,
    val group: Int,
    val payment: Int,
    val dates: String
)

@Serializable
data class LessonReportDto(
    val dateTime: String,
    val students: List<String>,
    val amount: Double,
    val forceGroup: Boolean,
    val extraHalfHour: Boolean,
    val photoReportLink: String?
)

@Serializable
data class PaymentReportDto(
    val dateTime: String,
    val studentFio: String,
    val amount: Int,
    val photoReportLink: String?
)

@Serializable
data class TutorSubjectDto(
    val tutorId: Long,
    val tutorName: String,
    val subjects: List<SubjectDto>
)

@Serializable
data class SubjectDto(
    val subjectId: Long,
    val subjectName: String
)

fun Route.reportApi() {
    route("/api/report") {
        get("/subjects") {
            val tutorId = extractAndValidateTelegramUser(
                call.request.headers["Authorization"],
                0,
                "GET /subjects"
            )

            if (tutorId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authorization required"))
                return@get
            }

            val tutor = TutorMapper.findByIdOrNull(tutorId)
            if (tutor == null) {
                KSLog.warning("ReportApi.GET /subjects: Unknown user ${tutorId.id}")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@get
            }

            try {
                val subjects = tutor.subjects.mapNotNull { subjectId ->
                    SubjectMapper.getNameById(subjectId)?.let { name ->
                        SubjectDto(
                            subjectId = subjectId.id,
                            subjectName = name
                        )
                    }
                }

                KSLog.info("ReportApi.GET /subjects: User ${tutorId.id} loaded subjects list")
                call.respond(subjects)
            } catch (e: Exception) {
                KSLog.error("ReportApi.GET /subjects: Error retrieving subjects", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/{subjectId}/{period}") {
            val subjectIdParam = call.parameters["subjectId"]?.toLongOrNull()
            val period = call.parameters["period"] // format: "current" or "previous" or "YYYY-MM"

            if (subjectIdParam == null || period == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid parameters"))
                return@get
            }

            val tutorId = extractAndValidateTelegramUser(
                call.request.headers["Authorization"],
                0,
                "GET /report"
            )

            if (tutorId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authorization required"))
                return@get
            }

            val tutor = TutorMapper.findByIdOrNull(tutorId)
            if (tutor == null) {
                KSLog.warning("ReportApi.GET: Unknown user ${tutorId.id}")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@get
            }

            try {
                val subjectId = subjectIdParam.toSubjectId()

                val subjectName = SubjectMapper.getNameById(subjectId)
                if (subjectName == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Subject not found"))
                    return@get
                }

                val (lessons, payments, targetMonth) = when (period) {
                    "current" -> {
                        Triple(
                            LessonMapper.getCurrentMontTimes(tutorId, subjectId),
                            PaymentMapper.getCurrentMonthPayments(tutorId, subjectId),
                            YearMonth.now()
                        )
                    }
                    "previous" -> {
                        Triple(
                            LessonMapper.getPrevMonthTimes(tutorId, subjectId),
                            PaymentMapper.getPrevMonthPayments(tutorId, subjectId),
                            YearMonth.now().minusMonths(1)
                        )
                    }
                    else -> {
                        try {
                            val yearMonth = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"))
                            Triple(
                                LessonMapper.getTimesByMonth(tutorId, subjectId, yearMonth),
                                PaymentMapper.getPaymentsByMonth(tutorId, subjectId, yearMonth),
                                yearMonth
                            )
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid period format"))
                            return@get
                        }
                    }
                }

                if (lessons.isEmpty() && payments.isEmpty()) {
                    call.respond(
                        ReportDataDto(
                            tutorName = tutor.name,
                            subjectName = subjectName,
                            month = targetMonth.month.name,
                            year = targetMonth.year,
                            students = emptyList(),
                            totalIndividual = 0,
                            totalGroup = 0,
                            totalPayments = 0,
                            lessons = emptyList(),
                            payments = emptyList()
                        )
                    )
                    return@get
                }

                // Group lessons by student
                val studentLessons = lessons.groupBy { it.studentId }
                val id2lessons = lessons.groupBy { it.id }

                var totalIndividual = 0
                var totalGroup = 0

                val studentReports = studentLessons.map { (studentId, studentLessonsList) ->
                    val student = StudentMapper.findById(studentId)
                    val individual = studentLessonsList.count { id2lessons[it.id]!!.isIndividual() }
                    val group = studentLessonsList.count { id2lessons[it.id]!!.isGroup() }

                    totalIndividual += individual
                    totalGroup += group

                    val dates = studentLessonsList
                        .sortedBy { it.dateTime }
                        .groupBy { it.dateTime.toLocalDate() }
                        .map { (date, lessonsOnDate) ->
                            "${date.format(DateTimeFormatter.ofPattern("dd.MM"))}(${lessonsOnDate.size})"
                        }
                        .joinToString(",")

                    val paymentSum = PaymentMapper.getPaymentsSumForStudent(
                        tutorId,
                        subjectId,
                        studentId,
                        targetMonth.atDay(1).atStartOfDay()
                    )

                    StudentReportDto(
                        fio = student.fio(),
                        schoolClass = student.schoolClass?.value?.toString() ?: "",
                        individual = individual,
                        group = group,
                        payment = paymentSum.toInt(),
                        dates = dates
                    )
                }.sortedWith(compareBy({ it.schoolClass.toIntOrNull() ?: 999 }, { it.fio }))

                val totalPaymentsSum = PaymentMapper.getPaymentsSum(
                    tutorId,
                    subjectId,
                    targetMonth.atDay(1).atStartOfDay()
                )

                val lessonReports = lessons
                    .groupBy { it.id }
                    .map { (_, lessonGroup) ->
                        val lesson = lessonGroup.first()
                        LessonReportDto(
                            dateTime = lesson.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                            students = lessonGroup.map { StudentMapper.findById(it.studentId).fio() }.sorted(),
                            amount = lesson.amount,
                            forceGroup = lesson.forceGroup,
                            extraHalfHour = lesson.extraHalfHour,
                            photoReportLink = null // MinioService links would need special handling
                        )
                    }
                    .sortedWith(compareBy(
                        { LocalDate.parse(it.dateTime.substringBefore(" "), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) },
                        { it.students.minOrNull() }
                    ))

                val paymentReports = payments.map { payment ->
                    val student = StudentMapper.findById(payment.studentId)
                    PaymentReportDto(
                        dateTime = payment.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                        studentFio = student.fio(),
                        amount = payment.amount.amount,
                        photoReportLink = null
                    )
                }.sortedWith(compareBy(
                    { LocalDate.parse(it.dateTime.substringBefore(" "), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) },
                    { it.studentFio }
                ))

                val reportData = ReportDataDto(
                    tutorName = tutor.name,
                    subjectName = subjectName,
                    month = targetMonth.month.name,
                    year = targetMonth.year,
                    students = studentReports,
                    totalIndividual = totalIndividual,
                    totalGroup = totalGroup,
                    totalPayments = totalPaymentsSum.toInt(),
                    lessons = lessonReports,
                    payments = paymentReports
                )

                KSLog.info("ReportApi.GET: User ${tutorId.id} loaded report for subject $subjectIdParam, period $period")
                call.respond(reportData)
            } catch (e: Exception) {
                KSLog.error("ReportApi.GET: Error generating report", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
