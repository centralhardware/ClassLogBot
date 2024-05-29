package me.centralhardware.znatoki.telegram.statistic.telegram.report

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.entity.Organization
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.toClientIds
import me.centralhardware.znatoki.telegram.statistic.formatTime
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import me.centralhardware.znatoki.telegram.statistic.toId
import java.util.*

object DailyReport{

    fun report() {
        OrganizationMapper.getOwners()
            .asSequence()
            .map(Organization::id)
            .map(ServiceMapper::getIds)
            .flatten()
            .toList()
            .forEach { runBlocking { getReport(it) } }
    }

    suspend fun getReport(id: Long, sendTo: Long = id) {
        val times = ServiceMapper.getTodayTimes(id)
        if (times.isEmpty()) return

        bot.sendTextMessage(sendTo.toId(), "Занятия проведенные за сегодня")

        val id2times: Multimap<UUID, Service> = ArrayListMultimap.create()
        times.forEach { service: Service ->
            id2times.put(service.id, service)
        }

        id2times.asMap().values
            .sortedBy { it.first().dateTime }
            .forEach {
                bot.sendTextMessage(
                    sendTo.toId(),
                    """
                        Время: ${it.first().dateTime.formatTime()}
                        Предмет: ${ServicesMapper.getNameById(it.first().serviceId)}
                        ${OrganizationMapper.getById(it.first().organizationId)!!.clientName}: ${
                        it.toClientIds().joinToString(", ") { clientId -> ClientMapper.getFioById(clientId) }
                    }
                        Стоимость: ${it.first().amount}
                    """.trimIndent()
                )
            }
        bot.sendTextMessage(sendTo.toId(), "Проверьте правильность внесенных данных")
    }

}