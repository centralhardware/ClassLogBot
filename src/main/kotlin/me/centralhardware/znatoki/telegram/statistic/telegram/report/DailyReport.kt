package me.centralhardware.znatoki.telegram.statistic.telegram.report

import me.centralhardware.znatoki.telegram.statistic.entity.Organization
import me.centralhardware.znatoki.telegram.statistic.entity.toClientIds
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class DailyReport(
    private val serviceMapper: ServiceMapper,
    private val sender: TelegramSender,
    private val organizationMapper: OrganizationMapper,
    private val servicesMapper: ServicesMapper,
    private val clientService: ClientService
) {
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @Scheduled(cron = "0 0 22 * * *")
    fun report() {
        organizationMapper.getOwners()
            .asSequence()
            .map(Organization::id)
            .map(serviceMapper::getIds)
            .flatten()
            .map(serviceMapper::getTodayTimes)
            .filterNot { it.isEmpty() }
            .toList()
            .forEach {
                val id = it.first().chatId
                sender.sendText("Занятия проведенные за сегодня", id)
                val service = it.first()
                sender.sendText(
                    """
                        Время: ${timeFormatter.format(service.dateTime)}
                        Предмет: ${servicesMapper.getNameById(service.serviceId)}
                        ${organizationMapper.getById(service.organizationId)!!.clientName}: ${
                        it.toClientIds().joinToString(", ") { clientId -> clientService.getFioById(clientId) }
                    }
                        Стоимость: ${service.amount}
                    """.trimIndent(), id
                )
                sender.sendText("Проверьте правильность внесенных данных", id)
            }
    }

}