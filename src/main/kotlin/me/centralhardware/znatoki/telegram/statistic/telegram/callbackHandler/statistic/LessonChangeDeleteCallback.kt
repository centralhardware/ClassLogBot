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
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper

private const val ACTION_DELETE = "timeDelete"
private const val ACTION_RESTORE = "timeRestore"
private const val UUID_REGEX =
    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
private val timeToggleRegex = Regex("($ACTION_DELETE|$ACTION_RESTORE)-($UUID_REGEX)")

fun BehaviourContext.registerTimeToggleCallback() = onDataCallbackQuery(timeToggleRegex) { query ->
    val (action, idStr) = timeToggleRegex.matchEntire(query.data)!!.destructured
    changeTimeStatus(
        idStr.toLessonId(),
        action == ACTION_DELETE,
        query)
}

private suspend fun BehaviourContext.changeTimeStatus(
    id: LessonId,
    deleted: Boolean,
    query: DataCallbackQuery
) {
    LessonMapper.setDeleted(id, deleted)
    val times = LessonMapper.findById(id)
    val current = times.firstOrNull() ?: return

    query.messageDataCallbackQueryOrNull()?.message?.let {
        edit(it, replyMarkup = buildTimeKeyboard(
            id = id,
            deleted = deleted,
            extraHalfHour = current.extraHalfHour,
            forceGroup = current.forceGroup,
            isSingle = times.size == 1))
    }
}

private fun buildTimeKeyboard(id: LessonId, deleted: Boolean, extraHalfHour: Boolean, forceGroup: Boolean, isSingle: Boolean) = inlineKeyboard {
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
