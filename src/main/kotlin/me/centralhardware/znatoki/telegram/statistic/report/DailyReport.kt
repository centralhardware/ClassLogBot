package me.centralhardware.znatoki.telegram.statistic.report

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import dev.inmo.krontab.buildSchedule
import dev.inmo.krontab.utils.asTzFlowWithDelays
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentIds
import me.centralhardware.znatoki.telegram.statistic.extensions.formatTime
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper

suspend fun BehaviourContext.dailyReport() {
    buildSchedule("0 0 22 * * *").asTzFlowWithDelays().collect {
        LessonMapper.getTutorIds().forEach { getReport(it) }
    }
}

suspend fun BehaviourContext.getReport(tutorId: TutorId, sendTo: TutorId = tutorId) {
    val lessons = LessonMapper.getTodayTimes(tutorId)
    if (lessons.isEmpty()) return

    sendTextMessage(sendTo.toChatId(), "Занятия проведенные за сегодня")

    val id2lessons: Multimap<LessonId, Lesson> = ArrayListMultimap.create()
    lessons.forEach { lesson: Lesson -> id2lessons.put(lesson.id, lesson) }

    id2lessons
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
                    it.toStudentIds().joinToString(", ") { studentId -> StudentMapper.getFioById(studentId) }
                }
                        Стоимость: ${it.first().amount}
                    """
                    .trimIndent(),
            )
        }
    sendTextMessage(sendTo.toChatId(), "Проверьте правильность внесенных данных")
}
