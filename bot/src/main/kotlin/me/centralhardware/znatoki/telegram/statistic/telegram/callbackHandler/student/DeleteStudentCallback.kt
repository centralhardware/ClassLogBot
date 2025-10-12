package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper
import me.centralhardware.znatoki.telegram.statistic.service.DiffService

fun BehaviourContext.deleteStudentCallback() = onDataCallbackQuery(Regex("delete_user\\d+$")) {
    val id = it.data.replace("delete_user", "").toInt().toStudentId()
    val student = StudentMapper.findById(id)
    StudentMapper.delete(id)

    AuditLogMapper.log(
        userId = it.user.id.chatId.long,
        action = "DELETE_STUDENT",
        entityType = "student",
        entityId = id.id,
        studentId = id.id,
        subjectId = null,
        student,
        null
    )
    
    sendMessage(it.from, "Ученик удален")
}
