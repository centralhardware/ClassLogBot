package me.centralhardware.znatoki.telegram.statistic.report

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import dev.inmo.krontab.buildSchedule
import dev.inmo.krontab.utils.asTzFlowWithDelays
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.toChatId
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentIds
import me.centralhardware.znatoki.telegram.statistic.extensions.formatTime
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import java.util.*

suspend fun BehaviourContext.dailyReport() {
    buildSchedule("0 0 22 * * *").asTzFlowWithDelays().collect {
        LessonMapper.getTutorIds().forEach { getReport(it) }
    }
}

suspend fun BehaviourContext.getReport(id: Long, sendTo: Long = id) {
    val times = LessonMapper.getTodayTimes(id)
    if (times.isEmpty()) return

    sendTextMessage(sendTo.toChatId(), "Занятия проведенные за сегодня")

    val id2times: Multimap<UUID, Lesson> = ArrayListMultimap.create()
    times.forEach { lesson: Lesson -> id2times.put(lesson.id.id, lesson) }

    id2times
        .asMap()
        .values
        .sortedBy { it.first().dateTime }
        .forEach {
            sendTextMessage(
                sendTo.toChatId(),
                """
                        Время: ${it.first().dateTime.formatTime()}
                        Предмет: ${SubjectMapper.getNameById(it.first().subjectId)}
                        ученик: ${
                    it.toStudentIds().joinToString(", ") { clientId -> StudentMapper.getFioById(clientId) }
                }
                        Стоимость: ${it.first().amount}
                    """
                    .trimIndent(),
            )
        }
    sendTextMessage(sendTo.toChatId(), "Проверьте правильность внесенных данных")
}
