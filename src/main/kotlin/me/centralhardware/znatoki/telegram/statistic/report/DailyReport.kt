package me.centralhardware.znatoki.telegram.statistic.report

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import dev.inmo.krontab.doOnceTz
import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.types.toChatId
import java.util.*
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.toClientIds
import me.centralhardware.znatoki.telegram.statistic.formatTime
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ConfigMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper

suspend fun dailyReport() {
    doOnceTz("0 0 22 * * *") { ServiceMapper.getIds().forEach { runBlocking { getReport(it) } } }
}

suspend fun getReport(id: Long, sendTo: Long = id) {
    val times = ServiceMapper.getTodayTimes(id)
    if (times.isEmpty()) return

    Trace.save("checkTimes", mapOf("id" to id.toString()))
    bot.sendTextMessage(sendTo.toChatId(), "Занятия проведенные за сегодня")

    val id2times: Multimap<UUID, Service> = ArrayListMultimap.create()
    times.forEach { service: Service -> id2times.put(service.id, service) }

    id2times
        .asMap()
        .values
        .sortedBy { it.first().dateTime }
        .forEach {
            bot.sendTextMessage(
                sendTo.toChatId(),
                """
                        Время: ${it.first().dateTime.formatTime()}
                        Предмет: ${ServicesMapper.getNameById(it.first().serviceId)}
                        ${ConfigMapper.clientName()}: ${
                    it.toClientIds().joinToString(", ") { clientId -> ClientMapper.getFioById(clientId) }
                }
                        Стоимость: ${it.first().amount}
                    """
                    .trimIndent(),
            )
        }
    bot.sendTextMessage(sendTo.toChatId(), "Проверьте правильность внесенных данных")
}
