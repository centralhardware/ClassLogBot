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
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.service.DiffService

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
        query)
}

private suspend fun BehaviourContext.changePaymentDelete(
    id: PaymentId,
    delete: Boolean,
    query: DataCallbackQuery
) {
    PaymentMapper.setDelete(id, delete)

    query.messageDataCallbackQueryOrNull()?.message?.let { msg ->
        edit(msg, replyMarkup = paymentKeyboard(id = id, deleted = delete))
    }
    
    // Audit log
    val payment = PaymentMapper.findById(id) ?: return
    val htmlDiff = if (delete) {
        DiffService.generateHtmlDiff(oldObj = payment, newObj = null)
    } else {
        DiffService.generateHtmlDiff(oldObj = null, newObj = payment)
    }
    
    AuditLogMapper.log(
        userId = query.user.id.chatId.long,
        action = if (delete) "DELETE_PAYMENT" else "RESTORE_PAYMENT",
        entityType = "payment",
        entityId = id.id,
        details = htmlDiff,
        studentId = payment.studentId.id,
        subjectId = payment.subjectId.id.toInt()
    )
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
