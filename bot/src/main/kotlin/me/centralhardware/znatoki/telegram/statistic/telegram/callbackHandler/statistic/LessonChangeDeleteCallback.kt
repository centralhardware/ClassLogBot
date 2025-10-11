package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.toLessonId
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.service.DiffService

private const val ACTION_DELETE = "timeDelete"
private const val ACTION_RESTORE = "timeRestore"
private const val UUID_REGEX =
    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
private val timeToggleRegex = Regex("($ACTION_DELETE|$ACTION_RESTORE)-($UUID_REGEX)")

fun BehaviourContext.registerLessonChangeDeleteCallback() = onDataCallbackQuery(timeToggleRegex) { query ->
    val (action, idStr) = timeToggleRegex.matchEntire(query.data)!!.destructured
    changeLessonDelete(
        idStr.toLessonId(),
        action == ACTION_DELETE,
        query)
}

private suspend fun BehaviourContext.changeLessonDelete(
    id: LessonId,
    deleted: Boolean,
    query: DataCallbackQuery
) {
    LessonMapper.setDeleted(id, deleted)
    val lessons = LessonMapper.findById(id)
    val lesson = lessons.firstOrNull() ?: return

    query.messageDataCallbackQueryOrNull()?.message?.let {
        edit(it, replyMarkup = buildLessonKeyboard(
            id = id,
            deleted = deleted,
            extraHalfHour = lesson.extraHalfHour,
            forceGroup = lesson.forceGroup,
            isSingle = lessons.size == 1))
    }
    
    val htmlDiff = if (deleted) {
        DiffService.generateHtmlDiff(oldObj = lesson, newObj = null)
    } else {
        DiffService.generateHtmlDiff(oldObj = null, newObj = lesson)
    }
    
    AuditLogMapper.log(
        userId = query.user.id.chatId.long,
        action = if (deleted) "DELETE_LESSON" else "RESTORE_LESSON",
        entityType = "lesson",
        entityId = null,
        details = htmlDiff,
        studentId = lesson.studentId.id,
        subjectId = lesson.subjectId.id.toInt()
    )
}

private fun buildLessonKeyboard(id: LessonId, deleted: Boolean, extraHalfHour: Boolean, forceGroup: Boolean, isSingle: Boolean) = inlineKeyboard {
    row {
        if (deleted) {
            dataButton("Восстановить", "$ACTION_RESTORE-${id.id}")
        } else {
            dataButton("Удалить", "$ACTION_DELETE-${id.id}")
        }
    }
    row {
        if (extraHalfHour) {
            dataButton("Убрать полтора часа", "extraHalfHourRemove-${id.id}")
        } else {
            dataButton("Сделать полтора часа", "extraHalfHourAdd-${id.id}")
        }
    }
    if (isSingle) {
        row {
            if (forceGroup) {
                dataButton("Сделать одиночным занятием", "forceGroupRemove-${id.id}")
            } else {
                dataButton("Сделать групповым занятием", "forceGroupAdd-${id.id}")
            }
        }
    }
}
