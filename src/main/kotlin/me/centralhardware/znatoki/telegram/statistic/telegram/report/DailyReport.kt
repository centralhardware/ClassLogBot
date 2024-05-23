package me.centralhardware.znatoki.telegram.statistic.telegram.report

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import me.centralhardware.znatoki.telegram.statistic.entity.Organization
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.toClientIds
import me.centralhardware.znatoki.telegram.statistic.formatTime
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

@Component
class DailyReport(
    private val serviceMapper: ServiceMapper,
    private val sender: TelegramSender,
    private val organizationMapper: OrganizationMapper,
    private val servicesMapper: ServicesMapper,
    private val clientService: ClientService
) {

    @Scheduled(cron = "0 0 22 * * *")
    fun report() {
        organizationMapper.getOwners()
            .asSequence()
            .map(Organization::id)
            .map(serviceMapper::getIds)
            .flatten()
            .toList()
            .forEach { getReport(it) }
    }

    fun getReport(id: Long, sendTo: Long = id) {
        val times = serviceMapper.getTodayTimes(id)
        if (times.isEmpty()) return

        sender.sendText("Занятия проведенные за сегодня", sendTo)

        val id2times: Multimap<UUID, Service> = ArrayListMultimap.create()
        times.forEach { service: Service ->
            id2times.put(service.id, service)
        }

        id2times.asMap().values
            .sortedBy { it.first().dateTime }
            .forEach {
                sender.sendText(
                    """
                        Время: ${it.first().dateTime.formatTime()}
                        Предмет: ${servicesMapper.getNameById(it.first().serviceId)}
                        ${organizationMapper.getById(it.first().organizationId)!!.clientName}: ${
                        it.toClientIds().joinToString(", ") { clientId -> clientService.getFioById(clientId) }
                    }
                        Стоимость: ${it.first().amount}
                    """.trimIndent(), sendTo
                )
            }
        sender.sendText("Проверьте правильность внесенных данных", sendTo)
    }

}