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

        val user = userMapper.getById(times.first().chatId)?.let {
            it.services.mapNotNull { service ->
                val date = times.stream()
                    .filter { time -> time.serviceId == service }
                    .findFirst()
                    .orElse(null)

                date?.let { _date ->
                    MonthReport(
                        it.name,
                        service,
                        servicesMapper.getKeyById(service)!!,
                        _date.dateTime,
                        organizationMapper.getReportFields(it.organizationId),
                        organizationMapper.getById(it.organizationId)!!.clientName,
                        id
                    ).generate(times)
                }
            }
        }

        return user ?: emptyList()
    }
}