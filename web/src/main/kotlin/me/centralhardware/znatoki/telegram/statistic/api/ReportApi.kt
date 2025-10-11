package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import me.centralhardware.znatoki.telegram.statistic.exception.BadRequestException
import me.centralhardware.znatoki.telegram.statistic.exception.NotFoundException
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.extensions.hasAdminPermission
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private data class Tuple6<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)

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
    val studentId: Int,
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
data class AggregatedStatsDto(
    val period: PeriodStatsDto,
    val previousPeriod: PeriodStatsDto,
    val comparison: ComparisonDto
)

@Serializable
data class PeriodStatsDto(
    val month: String,
    val year: Int,
    val totalLessons: Int,
    val totalIndividual: Int,
    val totalGroup: Int,
    val totalPayments: Int,
    val totalStudents: Int,
    val subjectStats: List<SubjectStatsDto>
)

@Serializable
data class SubjectStatsDto(
    val subjectName: String,
    val lessons: Int,
    val individual: Int,
    val group: Int,
    val payments: Int,
    val students: Int
)

@Serializable
data class ComparisonDto(
    val lessonsChange: Int,
    val lessonsChangePercent: Double,
    val paymentsChange: Int,
    val paymentsChangePercent: Double,
    val studentsChange: Int,
    val studentsChangePercent: Double
)

fun Route.reportApi() {
    route("/api/report") {
        get("/{subjectId}/{period}") {
            val subjectIdParam = call.parameters["subjectId"]?.toLongOrNull()
            val period = call.parameters["period"] // format: "YYYY-MM"
            val tutorIdParam = call.request.queryParameters["tutorId"]?.toLongOrNull()

            if (subjectIdParam == null || period == null) {
                throw BadRequestException("Invalid parameters: subjectId=$subjectIdParam, period=$period")
            }

            val requestingTutorId = call.authenticatedTutorId
            val requestingTutor = TutorMapper.findByIdOrNull(requestingTutorId)!!

            // Determine which tutor's report to show
            val targetTutorId = if (tutorIdParam != null && requestingTutor.hasAdminPermission()) {
                TutorId(tutorIdParam)
            } else {
                requestingTutorId
            }

            val tutor = TutorMapper.findByIdOrNull(targetTutorId)
                ?: throw NotFoundException("Tutor not found: ${targetTutorId.id}")

            val subjectId = subjectIdParam.toSubjectId()

            val subjectName = SubjectMapper.getNameById(subjectId)
                ?: throw NotFoundException("Subject not found: ${subjectId.id}")

            val targetMonth = try {
                YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"))
            } catch (e: Exception) {
                throw BadRequestException("Invalid period format: $period")
            }

            val lessons = LessonMapper.getTimesByMonth(targetTutorId, subjectId, targetMonth)
            val payments = PaymentMapper.getPaymentsByMonth(targetTutorId, subjectId, targetMonth)

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
                    targetTutorId,
                    subjectId,
                    studentId,
                    targetMonth.atDay(1).atStartOfDay()
                )

                StudentReportDto(
                    studentId = studentId.id,
                    fio = student.fio(),
                    schoolClass = student.schoolClass?.value?.toString() ?: "",
                    individual = individual,
                    group = group,
                    payment = paymentSum.toInt(),
                    dates = dates
                )
            }.sortedWith(compareBy({ it.schoolClass.toIntOrNull() ?: 999 }, { it.fio }))

            val totalPaymentsSum = PaymentMapper.getPaymentsSum(
                targetTutorId,
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
                .sortedWith(
                    compareBy(
                    {
                        LocalDate.parse(
                            it.dateTime.substringBefore(" "),
                            DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        )
                    },
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
            }.sortedWith(
                compareBy(
                { LocalDate.parse(it.dateTime.substringBefore(" "), DateTimeFormatter.ofPattern("dd.MM.yyyy")) },
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

            KSLog.info("ReportApi.GET: User ${requestingTutorId.id} loaded report for tutor ${targetTutorId.id}, subject $subjectIdParam, period $period")
            call.respond(reportData)
        }

        // Aggregated stats by subject
        get("/{subjectId}/aggregated/{period}") {
            val subjectIdParam = call.parameters["subjectId"]?.toLongOrNull()
            val period = call.parameters["period"]
            val tutorIdParam = call.request.queryParameters["tutorId"]?.toLongOrNull()

            if (subjectIdParam == null || period == null) {
                throw BadRequestException("Invalid parameters: subjectId=$subjectIdParam, period=$period")
            }

            val requestingTutorId = call.authenticatedTutorId
            val requestingTutor = TutorMapper.findByIdOrNull(requestingTutorId)!!

            // Determine which tutor's stats to show
            val targetTutorId = if (tutorIdParam != null && requestingTutor.hasAdminPermission()) {
                TutorId(tutorIdParam)
            } else {
                requestingTutorId
            }

            val tutor = TutorMapper.findByIdOrNull(targetTutorId)
                ?: throw NotFoundException("Tutor not found: ${targetTutorId.id}")

            val subjectId = subjectIdParam.toSubjectId()
            val subjectName = SubjectMapper.getNameById(subjectId)
                ?: throw NotFoundException("Subject not found: ${subjectId.id}")

            val currentMonth = try {
                YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"))
            } catch (e: Exception) {
                throw BadRequestException("Invalid period format: $period")
            }

            val previousMonth = currentMonth.minusMonths(1)

            val currentStartDate = currentMonth.atDay(1).atStartOfDay()
            val currentEndDate = currentMonth.atEndOfMonth().atTime(23, 59, 59)

            val previousStartDate = previousMonth.atDay(1).atStartOfDay()
            val previousEndDate =
                previousMonth.atDay(minOf(currentMonth.lengthOfMonth(), previousMonth.lengthOfMonth()))
                    .atTime(23, 59, 59)

            val currentStats = calculateSubjectPeriodStats(
                targetTutorId,
                subjectId,
                subjectName,
                currentMonth,
                currentStartDate,
                currentEndDate
            )

            val previousStats = calculateSubjectPeriodStats(
                targetTutorId,
                subjectId,
                subjectName,
                previousMonth,
                previousStartDate,
                previousEndDate
            )

            val comparison = ComparisonDto(
                lessonsChange = currentStats.totalLessons - previousStats.totalLessons,
                lessonsChangePercent = if (previousStats.totalLessons > 0)
                    ((currentStats.totalLessons - previousStats.totalLessons).toDouble() / previousStats.totalLessons * 100)
                else 0.0,
                paymentsChange = currentStats.totalPayments - previousStats.totalPayments,
                paymentsChangePercent = if (previousStats.totalPayments > 0)
                    ((currentStats.totalPayments - previousStats.totalPayments).toDouble() / previousStats.totalPayments * 100)
                else 0.0,
                studentsChange = currentStats.totalStudents - previousStats.totalStudents,
                studentsChangePercent = if (previousStats.totalStudents > 0)
                    ((currentStats.totalStudents - previousStats.totalStudents).toDouble() / previousStats.totalStudents * 100)
                else 0.0
            )

            val aggregatedStats = AggregatedStatsDto(
                period = currentStats,
                previousPeriod = previousStats,
                comparison = comparison
            )

            KSLog.info("ReportApi.GET /{subjectId}/aggregated: User ${requestingTutorId.id} loaded aggregated stats for tutor ${targetTutorId.id}, subject $subjectIdParam, period $period")
            call.respond(aggregatedStats)
        }

        // Aggregated stats for all subjects
        get("/aggregated/{period}") {
            val period = call.parameters["period"]
            val tutorIdParam = call.request.queryParameters["tutorId"]?.toLongOrNull()

            if (period == null) {
                throw BadRequestException("Invalid parameters: period is required")
            }

            val requestingTutorId = call.authenticatedTutorId
            val requestingTutor = TutorMapper.findByIdOrNull(requestingTutorId)!!

            // Determine which tutor's stats to show
            val targetTutorId = if (tutorIdParam != null && requestingTutor.hasAdminPermission()) {
                TutorId(tutorIdParam)
            } else {
                requestingTutorId
            }

            val tutor = TutorMapper.findByIdOrNull(targetTutorId)
                ?: throw NotFoundException("Tutor not found: ${targetTutorId.id}")

            val currentMonth = try {
                YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"))
            } catch (e: Exception) {
                throw BadRequestException("Invalid period format: $period")
            }

            val previousMonth = currentMonth.minusMonths(1)

            val currentStartDate = currentMonth.atDay(1).atStartOfDay()
            val currentEndDate = currentMonth.atEndOfMonth().atTime(23, 59, 59)

            val previousStartDate = previousMonth.atDay(1).atStartOfDay()
            val previousEndDate =
                previousMonth.atDay(minOf(currentMonth.lengthOfMonth(), previousMonth.lengthOfMonth()))
                    .atTime(23, 59, 59)

            val currentStats =
                calculatePeriodStats(targetTutorId, tutor, currentMonth, currentStartDate, currentEndDate)

            val previousStats =
                calculatePeriodStats(targetTutorId, tutor, previousMonth, previousStartDate, previousEndDate)

            val comparison = ComparisonDto(
                lessonsChange = currentStats.totalLessons - previousStats.totalLessons,
                lessonsChangePercent = if (previousStats.totalLessons > 0)
                    ((currentStats.totalLessons - previousStats.totalLessons).toDouble() / previousStats.totalLessons * 100)
                else 0.0,
                paymentsChange = currentStats.totalPayments - previousStats.totalPayments,
                paymentsChangePercent = if (previousStats.totalPayments > 0)
                    ((currentStats.totalPayments - previousStats.totalPayments).toDouble() / previousStats.totalPayments * 100)
                else 0.0,
                studentsChange = currentStats.totalStudents - previousStats.totalStudents,
                studentsChangePercent = if (previousStats.totalStudents > 0)
                    ((currentStats.totalStudents - previousStats.totalStudents).toDouble() / previousStats.totalStudents * 100)
                else 0.0
            )

            val aggregatedStats = AggregatedStatsDto(
                period = currentStats,
                previousPeriod = previousStats,
                comparison = comparison
            )

            KSLog.info("ReportApi.GET /aggregated: User ${requestingTutorId.id} loaded aggregated stats for tutor ${targetTutorId.id}, period $period")
            call.respond(aggregatedStats)
        }
    }
}

private fun calculatePeriodStats(
    tutorId: TutorId,
    tutor: me.centralhardware.znatoki.telegram.statistic.entity.Tutor,
    yearMonth: YearMonth,
    startDate: LocalDateTime,
    endDate: LocalDateTime
): PeriodStatsDto {
    var totalLessons = 0
    var totalIndividual = 0
    var totalGroup = 0
    var totalPayments = 0
    val allStudents = mutableSetOf<me.centralhardware.znatoki.telegram.statistic.entity.StudentId>()

    val subjectStats = tutor.subjects.mapNotNull { subjectId ->
        val subjectName = SubjectMapper.getNameById(subjectId) ?: return@mapNotNull null

        val lessons = LessonMapper.getTimesByDateRange(tutorId, subjectId, startDate, endDate)
        val payments = PaymentMapper.getPaymentsByDateRange(tutorId, subjectId, startDate, endDate)

        if (lessons.isEmpty() && payments.isEmpty()) {
            return@mapNotNull null
        }

        val id2lessons = lessons.groupBy { it.id }
        val individual = lessons.count { id2lessons[it.id]!!.isIndividual() }
        val group = lessons.count { id2lessons[it.id]!!.isGroup() }
        val paymentsSum = PaymentMapper.getPaymentsSumByDateRange(tutorId, subjectId, startDate, endDate).toInt()
        val students = lessons.map { it.studentId }.toSet()

        totalLessons += lessons.size
        totalIndividual += individual
        totalGroup += group
        totalPayments += paymentsSum
        allStudents.addAll(students)

        SubjectStatsDto(
            subjectName = subjectName,
            lessons = lessons.size,
            individual = individual,
            group = group,
            payments = paymentsSum,
            students = students.size
        )
    }

    return PeriodStatsDto(
        month = yearMonth.month.name,
        year = yearMonth.year,
        totalLessons = totalLessons,
        totalIndividual = totalIndividual,
        totalGroup = totalGroup,
        totalPayments = totalPayments,
        totalStudents = allStudents.size,
        subjectStats = subjectStats
    )
}

private fun calculateSubjectPeriodStats(
    tutorId: TutorId,
    subjectId: SubjectId,
    subjectName: String,
    yearMonth: YearMonth,
    startDate: LocalDateTime,
    endDate: LocalDateTime
): PeriodStatsDto {
    val lessons = LessonMapper.getTimesByDateRange(tutorId, subjectId, startDate, endDate)
    val payments = PaymentMapper.getPaymentsByDateRange(tutorId, subjectId, startDate, endDate)

    if (lessons.isEmpty() && payments.isEmpty()) {
        return PeriodStatsDto(
            month = yearMonth.month.name,
            year = yearMonth.year,
            totalLessons = 0,
            totalIndividual = 0,
            totalGroup = 0,
            totalPayments = 0,
            totalStudents = 0,
            subjectStats = emptyList()
        )
    }

    val id2lessons = lessons.groupBy { it.id }
    val individual = lessons.count { id2lessons[it.id]!!.isIndividual() }
    val group = lessons.count { id2lessons[it.id]!!.isGroup() }
    val paymentsSum = PaymentMapper.getPaymentsSumByDateRange(tutorId, subjectId, startDate, endDate).toInt()
    val students = lessons.map { it.studentId }.toSet()

    val subjectStats = listOf(
        SubjectStatsDto(
            subjectName = subjectName,
            lessons = lessons.size,
            individual = individual,
            group = group,
            payments = paymentsSum,
            students = students.size
        )
    )

    return PeriodStatsDto(
        month = yearMonth.month.name,
        year = yearMonth.year,
        totalLessons = lessons.size,
        totalIndividual = individual,
        totalGroup = group,
        totalPayments = paymentsSum,
        totalStudents = students.size,
        subjectStats = subjectStats
    )
}
