package me.centralhardware.znatoki.telegram.statistic.report

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import dev.inmo.krontab.buildSchedule
import dev.inmo.krontab.utils.asTzFlowWithDelays
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.toChatId
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.toClientIds
import me.centralhardware.znatoki.telegram.statistic.extensions.formatTime
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ConfigMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import java.util.*

suspend fun BehaviourContext.dailyReport() {
    buildSchedule("0 0 22 * * *").asTzFlowWithDelays().collect {
        ServiceMapper.getIds().forEach { getReport(it) }
    }
}

suspend fun BehaviourContext.getReport(id: Long, sendTo: Long = id) {
    val times = ServiceMapper.getTodayTimes(id)
    if (times.isEmpty()) return

    sendTextMessage(sendTo.toChatId(), "Занятия проведенные за сегодня")

    val id2times: Multimap<UUID, Service> = ArrayListMultimap.create()
    times.forEach { service: Service -> id2times.put(service.id, service) }

    id2times
        .asMap()
        .values
        .sortedBy { it.first().dateTime }
        .forEach {
            sendTextMessage(
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
    sendTextMessage(sendTo.toChatId(), "Проверьте правильность внесенных данных")
}
