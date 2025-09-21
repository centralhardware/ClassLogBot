package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper

private const val ACTION_DELETE = "paymentDelete"
private const val ACTION_RESTORE = "paymentRestore"
private val paymentRegex = Regex("($ACTION_DELETE|$ACTION_RESTORE)-(\\d+)")

fun BehaviourContext.registerPaymentToggleCallback() = onDataCallbackQuery(paymentRegex) { query ->
    val match = paymentRegex.matchEntire(query.data) ?: return@onDataCallbackQuery
    val (action, idStr) = match.destructured
    val id = idStr.toIntOrNull() ?: return@onDataCallbackQuery

    val delete = action == ACTION_DELETE
    changePaymentStatus(id, delete, query)
}

private suspend fun BehaviourContext.changePaymentStatus(
    id: Int,
    delete: Boolean,
    query: DataCallbackQuery
) {
    PaymentMapper.setDelete(id, delete)

    query.messageDataCallbackQueryOrNull()?.message?.let { msg ->
        edit(msg, replyMarkup = paymentKeyboard(id = id, deleted = delete))
    }
}

private fun paymentKeyboard(id: Int, deleted: Boolean) = inlineKeyboard {
    row {
        if (deleted) {
            dataButton("Восстановить", "$ACTION_RESTORE-$id")
        } else {
            dataButton("Удалить", "$ACTION_DELETE-$id")
        }
    }
}
