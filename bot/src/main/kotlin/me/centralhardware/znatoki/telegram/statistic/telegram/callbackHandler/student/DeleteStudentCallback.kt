package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student

import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.telegram.BotAudit
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.messageDataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.utils.row
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper

private val askRegex = Regex("^delete_user(\\d+)$")
private val confirmRegex = Regex("^delete_user_confirm(\\d+)$")
private const val CANCEL = "delete_user_cancel"

fun BehaviourContext.deleteStudentCallback() {
    onDataCallbackQuery(askRegex) { query ->
        val id = askRegex.matchEntire(query.data)!!.groupValues[1].toInt().toStudentId()
        answerCallbackQuery(query)
        sendMessage(
            query.from,
            "Удалить ученика ${StudentMapper.getFioById(id)}?",
            replyMarkup = inlineKeyboard {
                row {
                    dataButton("Да, удалить", "delete_user_confirm${id.id}")
                    dataButton("Отмена", CANCEL)
                }
            }
        )
    }

    onDataCallbackQuery(confirmRegex) { query ->
        val id = confirmRegex.matchEntire(query.data)!!.groupValues[1].toInt().toStudentId()
        StudentMapper.delete(id, TutorId(query.userId()))
        BotAudit.student(query.userId(), "Удалено", id, listOf(Change("удалён", false, true)))
        answerCallbackQuery(query, "Ученик удален")
        query.messageDataCallbackQueryOrNull()?.message?.let { edit(it, replyMarkup = null) }
    }

    onDataCallbackQuery(CANCEL) { query ->
        answerCallbackQuery(query, "Отменено")
        query.messageDataCallbackQueryOrNull()?.message?.let { edit(it, replyMarkup = null) }
    }
}
