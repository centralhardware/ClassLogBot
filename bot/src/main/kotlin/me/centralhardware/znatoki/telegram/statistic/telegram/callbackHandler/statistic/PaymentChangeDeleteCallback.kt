package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.entity.PaymentId
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper

private const val ACTION_DELETE = "paymentDelete"
private const val ACTION_RESTORE = "paymentRestore"
private val paymentRegex = Regex("($ACTION_DELETE|$ACTION_RESTORE)-(\\d+)")

fun BehaviourContext.registerPaymentChangeDeleteCallback() = onDataCallbackQuery(paymentRegex) { query ->
    val match = paymentRegex.matchEntire(query.data) ?: return@onDataCallbackQuery
    val (action, idStr) = match.destructured
    val id = idStr.toIntOrNull()?.let { PaymentId(it) } ?: return@onDataCallbackQuery

    changePaymentDelete(
        id,
        action == ACTION_DELETE,
        query
    )
}

private suspend fun BehaviourContext.changePaymentDelete(
    id: PaymentId,
    delete: Boolean,
    query: DataCallbackQuery
) {
    val payment = PaymentMapper.findByIdIncludingDeleted(id) ?: return

    PaymentMapper.setDelete(id, delete)

    query.messageDataCallbackQueryOrNull()?.message?.let { msg ->
        edit(msg, replyMarkup = paymentKeyboard(id = id, deleted = delete))
    }
    if (delete) {
        AuditLogMapper.log(
            userId = query.user.id.chatId.long,
            action = "DELETE_PAYMENT",
            entityType = "payment",
            entityId = id.id.toString(),
            studentId = payment.studentId.id,
            subjectId = payment.subjectId.id.toInt(),
            payment,
            null
        )
    } else {
        AuditLogMapper.log(
            userId = query.user.id.chatId.long,
            action = "RESTORE_PAYMENT",
            entityType = "payment",
            entityId = id.id.toString(),
            studentId = payment.studentId.id,
            subjectId = payment.subjectId.id.toInt(),
            null,
            payment
        )
    }


}

private fun paymentKeyboard(id: PaymentId, deleted: Boolean) = inlineKeyboard {
    row {
        if (deleted) {
            dataButton("Восстановить", "$ACTION_RESTORE-${id.id}")
        } else {
            dataButton("Удалить", "$ACTION_DELETE-${id.id}")
        }
    }
}
