package me.centralhardware.znatoki.telegram.statistic.service

import kotliquery.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport
import java.io.File
import java.time.YearMonth

object ReportService {

    context(_: Session)
    suspend fun getReportsCurrent(id: TutorId): List<File> {
        return getReport({ t, s -> LessonMapper.getCurrentMontTimes(t, s) }, { t, s -> PaymentMapper.getCurrentMonthPayments(t, s) }, id)
    }

    context(_: Session)
    suspend fun getReportPrevious(id: TutorId): List<File> {
        return getReport({ t, s -> LessonMapper.getPrevMonthTimes(t, s) }, { t, s -> PaymentMapper.getPrevMonthPayments(t, s) }, id)
    }

    context(_: Session)
    suspend fun getReportByMonth(id: TutorId, yearMonth: YearMonth): List<File> {
        return getReport(
            { tutorId, subjectId -> LessonMapper.getTimesByMonth(tutorId, subjectId, yearMonth) },
            { tutorId, subjectId -> PaymentMapper.getPaymentsByMonth(tutorId, subjectId, yearMonth) },
            id
        )
    }

    /** One report file per subject that has lessons; the DB reads and Excel generation run on [Dispatchers.IO]. */
    context(_: Session)
    private suspend fun getReport(
        getTime: (TutorId, SubjectId) -> List<Lesson>,
        getPayments: (TutorId, SubjectId) -> List<Payment>,
        id: TutorId,
    ): List<File> = withContext(Dispatchers.IO) {
        val user = TutorMapper.findByIdOrNull(id) ?: return@withContext emptyList()
        user.subjects.mapNotNull { subjectId ->
            // One after the other: both run on the caller's session, and a JDBC connection takes one query at a time.
            val lessons = getTime(id, subjectId)
            if (lessons.none { it.subjectId == subjectId }) return@mapNotNull null

            MonthReport(user.name, subjectId, id).generate(lessons, getPayments(id, subjectId))
        }
    }
}
