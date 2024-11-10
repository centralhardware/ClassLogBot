package me.centralhardware.znatoki.telegram.statistic.service

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport
import java.io.File

object ReportService {

    fun getReportsCurrent(id: Long): List<File> {
        return getReport(ServiceMapper::getCurrentMontTimes, PaymentMapper::getCurrentMonthPayments, id)
    }

    fun getReportPrevious(id: Long): List<File> {
        return getReport(ServiceMapper::getPrevMonthTimes, PaymentMapper::getPrevMonthPayments, id)
    }

    private fun getReport(getTime: (Long, Long) -> List<Service>, getPayments: (Long, Long) -> List<Payment>, id: Long): List<File> {
        val user =
            UserMapper.findById(id)?.let { user ->
                user.services.mapNotNull { serviceId ->
                    runBlocking {
                        val times = async { getTime.invoke(id, serviceId) }
                        val payments = async { getPayments.invoke(id, serviceId) }

                        val service = times.await().firstOrNull() { time -> time.serviceId == serviceId }
                        if (service == null) return@runBlocking null

                        MonthReport(user.name, serviceId, service!! .dateTime, id).generate(times.await(), payments.await())
                    }
                }
            }

        return user ?: emptyList()
    }
}
