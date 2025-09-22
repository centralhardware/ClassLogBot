package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper

fun BehaviourContext.userInfoCallback() = onDataCallbackQuery(Regex("user_info\\d+$")) {
    StudentMapper.findById(it.data.replace("user_info", "").toInt().toStudentId())?.let { client ->
        val info =
            client.getInfo(
                LessonMapper.getSubjectIdsForStudent(client.id!!)
                    .mapNotNull { SubjectMapper.getNameById(it) }
                    .toList()
            )
        sendMessage(it.from, info, parseMode = MarkdownParseMode)
    } ?: sendMessage(it.from, "Пользователь не найден")
}
