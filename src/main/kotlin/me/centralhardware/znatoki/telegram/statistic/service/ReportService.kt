package me.centralhardware.znatoki.telegram.statistic.service

import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport
import org.apache.commons.collections4.CollectionUtils
import org.springframework.stereotype.Component
import java.io.File

@Component
class ReportService(
    private val serviceMapper: ServiceMapper,
    private val userMapper: UserMapper,
    private val servicesMapper: ServicesMapper,
    private val organizationMapper: OrganizationMapper
) {

    fun getReportsCurrent(id: Long): List<File> {
        return getReport(serviceMapper::getCuurentMontTimes, id)
    }

    fun getReportPrevious(id: Long): List<File> {
        return getReport(serviceMapper::getPrevMonthTimes, id)
    }

    private fun getReport(getTime: (Long) -> List<Service>, id: Long): List<File> {
        val times = getTime.invoke(id)
        if (CollectionUtils.isEmpty(times)) return emptyList()

        val user = userMapper.getById(times.first().chatId)?.let {user ->
            user.services.mapNotNull { serviceId ->
                val service = times.stream()
                    .filter { time -> time.serviceId == serviceId }
                    .findFirst()
                    .orElse(null)

                service?.let {
                    MonthReport(
                        user.name,
                        serviceId,
                        servicesMapper.getNameById(serviceId)!!,
                        it.dateTime,
                        organizationMapper.getReportFields(user.organizationId),
                        organizationMapper.getById(user.organizationId)!!.clientName,
                        id
                    ).generate(times)
                }
            }
        }

        return user ?: emptyList()
    }
}