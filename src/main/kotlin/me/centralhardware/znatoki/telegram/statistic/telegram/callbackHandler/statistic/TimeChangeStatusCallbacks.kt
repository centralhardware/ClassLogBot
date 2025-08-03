package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import java.util.*

private suspend fun BehaviourContext.changeTimeStatus(id: UUID, deleted: Boolean, query: DataCallbackQuery) {
    ServiceMapper.setDeleted(id, deleted)
    val times = ServiceMapper.findById(id)

    val keyboard = inlineKeyboard {
        if (deleted) {
            row { dataButton("Восстановить", "timeRestore-$id") }
        } else {
            row { dataButton("Удалить", "timeDelete-$id") }
        }
        if (times.first().extraHalfHour) {
            row { dataButton("Убрать полтора часа", "extraHalfHourRemove-${times.first().id}") }
        } else {
            row { dataButton("Сделать полтора часа", "extraHalfHourAdd-${times.first().id}") }
        }
        if (times.size == 1) {
            if (times.first().forceGroup) {
                row { dataButton("Сделать одиночным занятием", "forceGroupRemove-$id") }
            } else {
                row { dataButton("Сделать групповым занятием", "forceGroupAdd-$id") }
            }
        }
    }

    query.messageDataCallbackQueryOrNull()?.message?.let { edit(it, replyMarkup = keyboard) }
}

suspend fun BehaviourContext.timeRestoreCallback() =
    onDataCallbackQuery(Regex("timeRestore-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
        val id = UUID.fromString(it.data.replace("timeRestore-", ""))
        changeTimeStatus(id, false, it)
    }


suspend fun BehaviourContext.timeDeleteCallback() =
    onDataCallbackQuery(Regex("timeDelete-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
        val id = UUID.fromString(it.data.replace("timeDelete-", ""))
        changeTimeStatus(id, true, it)
    }