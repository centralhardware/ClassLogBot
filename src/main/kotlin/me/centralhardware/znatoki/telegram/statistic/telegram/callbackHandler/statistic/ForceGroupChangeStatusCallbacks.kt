package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.extensions.hasAdminPermission
import me.centralhardware.znatoki.telegram.statistic.extensions.hasExtraHalfHour
import me.centralhardware.znatoki.telegram.statistic.extensions.isDm
import me.centralhardware.znatoki.telegram.statistic.extensions.isInSameMonthAs
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.user
import java.time.LocalDateTime
import java.util.*

private suspend fun BehaviourContext.changeForceGroupStatus(id: UUID, forceGroup: Boolean, query: DataCallbackQuery) {
    val chatId = query.from.id.chatId.long
    val service = ServiceMapper.findById(id).first()
    if (!data.user.hasAdminPermission() && service.chatId != chatId) {
        answerCallbackQuery(query, "Доступ запрещен", showAlert = true)
        return
    }
    if (!service.dateTime.isInSameMonthAs(LocalDateTime.now())) {
        answerCallbackQuery(
            query,
            "Нельзя модифицировать запись после окончания месяца",
            showAlert = true,
        )
        return
    }

    ServiceMapper.setForceGroup(id, forceGroup)
    val times = ServiceMapper.findById(id)
    if (times.size > 1) {
        answerCallbackQuery(query, "Занятие уже групповое", showAlert = true)
        return
    }

    val keyboard = inlineKeyboard {
        if (!query.isDm()) {
            if (times.first().deleted) {
                row { dataButton("Восстановить", "timeRestore-$id") }
            } else {
                row { dataButton("Удалить", "timeDelete-$id") }
            }
        }
        if (times.first().extraHalfHour && UserMapper.findById(chatId).hasExtraHalfHour()) {
            row { dataButton("Убрать полтора часа", "extraHalfHourRemove-${service.id}") }
        } else {
            row { dataButton("Сделать полтора часа", "extraHalfHourAdd-${service.id}") }
        }
        if (forceGroup) {
            row { dataButton("Сделать одиночным занятием", "forceGroupRemove-$id") }
        } else {
            row { dataButton("Сделать групповым занятием", "forceGroupAdd-$id") }
        }
    }

    query.messageDataCallbackQueryOrNull()
        ?.let { edit(it.message, replyMarkup = keyboard) }
}

fun BehaviourContext.forceGroupRemove() =
    onDataCallbackQuery(Regex("forceGroupRemove-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
        val id = UUID.fromString(it.data.replace("forceGroupRemove-", ""))
        changeForceGroupStatus(id, false, it)

}

fun BehaviourContext.forceGroupAdd() =
    onDataCallbackQuery(Regex("forceGroupAdd-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
        val id = UUID.fromString(it.data.replace("forceGroupAdd-", ""))
        changeForceGroupStatus(id, true, it)

    }