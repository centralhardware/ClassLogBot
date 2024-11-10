package me.centralhardware.znatoki.telegram.statistic.service

import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import java.io.File
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport
import org.apache.commons.collections4.CollectionUtils

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
                    val times = getTime.invoke(id, serviceId)
                    val payments = getPayments.invoke(id, serviceId)
                    if (CollectionUtils.isEmpty(times) && CollectionUtils.isEmpty(payments)) return emptyList()

                    val service = times.first { time -> time.serviceId == serviceId }

                    MonthReport(user.name, serviceId, service.dateTime, id).generate(times, payments)
                }
            }

        return user ?: emptyList()
    }
}
