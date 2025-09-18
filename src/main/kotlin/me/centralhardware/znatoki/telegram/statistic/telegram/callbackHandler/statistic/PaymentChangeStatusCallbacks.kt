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

private suspend fun BehaviourContext.changePaymentStatus(id: Int, delete: Boolean, query: DataCallbackQuery) {
    PaymentMapper.setDelete(id, delete)

    query.messageDataCallbackQueryOrNull()?.message?.let {
        edit(
            it,
            replyMarkup =
                inlineKeyboard {
                    row {
                        if (delete) {
                            dataButton("Восстановить", "paymentRestore-$id")
                        } else {
                            dataButton("Удалить", "paymentDelete-$id")
                        }
                    }
                },
        )
    }
}

fun BehaviourContext.paymentDeleteCallback() = onDataCallbackQuery(Regex($$"paymentDelete-\\d+$")) {
    val id = it.data.replace("paymentDelete-", "").toInt()
    changePaymentStatus(id, true, it)
}

fun BehaviourContext.paymentRestoreCallback() = onDataCallbackQuery(Regex($$"paymentRestore-\\d+$")) {
    val id = it.data.replace("paymentRestore-", "").toInt()
    changePaymentStatus(id, false, it)
}
