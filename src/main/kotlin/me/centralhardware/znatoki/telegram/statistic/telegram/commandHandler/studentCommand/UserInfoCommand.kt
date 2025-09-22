package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper

fun BehaviourContext.userInfoCommand() = onCommandWithArgs("i") { message, args ->
    StudentMapper.findById(args.first().toInt().toStudentId())?.let { client ->
        sendMessage(
            message.chat,
            client.getInfo(
                LessonMapper.getSubjectIdsForStudent(client.id!!)
                    .mapNotNull { SubjectMapper.getNameById(it) }
                    .toList()
            ),
        )
    } ?: sendMessage(message.chat, "Ученик не найден")
}
