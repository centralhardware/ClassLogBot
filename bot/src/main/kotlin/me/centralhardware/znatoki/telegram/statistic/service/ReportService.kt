package me.centralhardware.znatoki.telegram.statistic.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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

    suspend fun getReportsCurrent(id: TutorId): List<File> {
        return getReport(LessonMapper::getCurrentMontTimes, PaymentMapper::getCurrentMonthPayments, id)
    }

    suspend fun getReportPrevious(id: TutorId): List<File> {
        return getReport(LessonMapper::getPrevMonthTimes, PaymentMapper::getPrevMonthPayments, id)
    }

    suspend fun getReportByMonth(id: TutorId, yearMonth: YearMonth): List<File> {
        return getReport(
            { tutorId, subjectId -> LessonMapper.getTimesByMonth(tutorId, subjectId, yearMonth) },
            { tutorId, subjectId -> PaymentMapper.getPaymentsByMonth(tutorId, subjectId, yearMonth) },
            id
        )
    }

    /** One report file per subject that has lessons; the DB reads and Excel generation run on [Dispatchers.IO]. */
    private suspend fun getReport(
        getTime: (TutorId, SubjectId) -> List<Lesson>,
        getPayments: (TutorId, SubjectId) -> List<Payment>,
        id: TutorId,
    ): List<File> = withContext(Dispatchers.IO) {
        val user = TutorMapper.findByIdOrNull(id) ?: return@withContext emptyList()
        user.subjects.mapNotNull { subjectId ->
            val times = async { getTime(id, subjectId) }
            val payments = async { getPayments(id, subjectId) }

            val lessons = times.await()
            if (lessons.none { it.subjectId == subjectId }) {
                payments.cancel()
                return@mapNotNull null
            }

            MonthReport(user.name, subjectId, id).generate(lessons, payments.await())
        }
    }
}
