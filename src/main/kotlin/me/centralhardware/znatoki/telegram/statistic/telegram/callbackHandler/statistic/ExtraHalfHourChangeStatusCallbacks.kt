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
import me.centralhardware.znatoki.telegram.statistic.extensions.hasForceGroup
import me.centralhardware.znatoki.telegram.statistic.extensions.isDm
import me.centralhardware.znatoki.telegram.statistic.extensions.isInSameMonthAs
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.user
import java.time.LocalDateTime
import java.util.UUID

private const val ACTION_ADD = "extraHalfHourAdd"
private const val ACTION_REMOVE = "extraHalfHourRemove"
private const val UUID_REGEX =
    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

private val toggleRegex = Regex("($ACTION_ADD|$ACTION_REMOVE)-($UUID_REGEX)")

/**
 * Единый хендлер двух действий: extraHalfHourAdd / extraHalfHourRemove
 */
fun BehaviourContext.registerExtraHalfHourHandlers() = onDataCallbackQuery(toggleRegex) { query ->
    val (action, idStr) = toggleRegex.matchEntire(query.data)!!.destructured
    val id = UUID.fromString(idStr)
    val enable = action == ACTION_ADD
    changeExtraHalfHour(id, enable, query)
}

private suspend fun BehaviourContext.changeExtraHalfHour(
    id: UUID,
    extraHalfHour: Boolean,
    query: DataCallbackQuery
) {
    val chatId = query.from.id.chatId.long
    val historyBefore = ServiceMapper.findById(id)
    val service = historyBefore.firstOrNull()
        ?: run {
            answerCallbackQuery(query, "Запись не найдена", showAlert = true)
            return
        }

    if (!data.user.hasAdminPermission() && service.chatId != chatId) {
        answerCallbackQuery(query, "Доступ запрещён", showAlert = true)
        return
    }
    if (!service.dateTime.isInSameMonthAs(LocalDateTime.now())) {
        answerCallbackQuery(query, "Нельзя модифицировать запись после окончания месяца", showAlert = true)
        return
    }

    ServiceMapper.setExtraHalfHour(id, extraHalfHour)

    val historyAfter = ServiceMapper.findById(id)
    val updated = historyAfter.firstOrNull() ?: service

    val keyboard = buildServiceKeyboard(
        id = id,
        isDm = query.isDm(),
        canForceGroup = data.user.hasForceGroup(),
        deleted = updated.deleted,
        extraHalfHour = updated.extraHalfHour,
        showForceGroupSwitcher = historyAfter.size == 1,
        forceGroup = updated.forceGroup,
    )

    query.messageDataCallbackQueryOrNull()
        ?.let { edit(it.message, replyMarkup = keyboard) }
}

private fun buildServiceKeyboard(
    id: UUID,
    isDm: Boolean,
    canForceGroup: Boolean,
    deleted: Boolean,
    extraHalfHour: Boolean,
    showForceGroupSwitcher: Boolean,
    forceGroup: Boolean
) = inlineKeyboard {
    if (!isDm) {
        if (deleted) {
            row { dataButton("Восстановить", "timeRestore-$id") }
        } else {
            row { dataButton("Удалить", "timeDelete-$id") }
        }
    }

    if (extraHalfHour) {
        row { dataButton("Убрать полтора часа", "$ACTION_REMOVE-$id") }
    } else {
        row { dataButton("Сделать полтора часа", "$ACTION_ADD-$id") }
    }

    if (showForceGroupSwitcher && canForceGroup) {
        if (forceGroup) {
            row { dataButton("Сделать одиночным занятием", "forceGroupRemove-$id") }
        } else {
            row { dataButton("Сделать групповым занятием", "forceGroupAdd-$id") }
        }
    }
}
