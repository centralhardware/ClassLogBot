package me.centralhardware.znatoki.telegram.statistic.service

import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport
import org.apache.commons.collections4.CollectionUtils
import java.io.File

object ReportService{

    fun getReportsCurrent(id: Long): List<File> {
        return getReport(ServiceMapper::getCurrentMontTimes, id)
    }

    fun getReportPrevious(id: Long): List<File> {
        return getReport(ServiceMapper::getPrevMonthTimes, id)
    }

    private fun getReport(getTime: (Long) -> List<Service>, id: Long): List<File> {
        val times = getTime.invoke(id)
        if (CollectionUtils.isEmpty(times)) return emptyList()

        val user = UserMapper.findById(times.first().chatId)?.let { user ->
            user.services.mapNotNull { serviceId ->
                val service = times.stream()
                    .filter { time -> time.serviceId == serviceId }
                    .findFirst()
                    .orElse(null)

                service?.let {
                    MonthReport(
                        user.name,
                        serviceId,
                        it.dateTime,
                        id
                    ).generate(times)
                }
            }
        }

        return user ?: emptyList()
    }
}