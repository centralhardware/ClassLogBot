package me.centralhardware.znatoki.telegram.statistic.service

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport
import java.io.File

object ReportService {

    fun getReportsCurrent(id: TutorId): List<File> {
        return getReport(LessonMapper::getCurrentMontTimes, PaymentMapper::getCurrentMonthPayments, id)
    }

    fun getReportPrevious(id: TutorId): List<File> {
        return getReport(LessonMapper::getPrevMonthTimes, PaymentMapper::getPrevMonthPayments, id)
    }

    private fun getReport(getTime: (TutorId, SubjectId) -> List<Lesson>, getPayments: (TutorId, SubjectId) -> List<Payment>, id: TutorId): List<File> {
        val user =
            TutorMapper.findByIdOrNull(id)?.let { user ->
                user.subjects.mapNotNull { subjectId ->
                    runBlocking {
                        val times = async { getTime.invoke(id, subjectId) }
                        val payments = async { getPayments.invoke(id, subjectId) }

                        val service = times.await().firstOrNull { time -> time.subjectId == subjectId }
                        if (service == null) return@runBlocking null

                        MonthReport(user.name, subjectId, id).generate(times.await(), payments.await())
                    }
                }
            }

        return user ?: emptyList()
    }
}
