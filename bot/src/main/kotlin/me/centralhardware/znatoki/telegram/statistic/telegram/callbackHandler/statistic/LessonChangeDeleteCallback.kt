package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic

import kotliquery.Session
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.extensions.db
import me.centralhardware.znatoki.telegram.statistic.telegram.BotAudit
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
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
import me.centralhardware.znatoki.telegram.statistic.extensions.hasAdminPermission
import me.centralhardware.znatoki.telegram.statistic.extensions.isInSameMonthAs
import me.centralhardware.znatoki.telegram.statistic.extensions.user
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import java.time.LocalDateTime


private const val ACTION_DELETE = "timeDelete"
private const val ACTION_RESTORE = "timeRestore"
private const val UUID_REGEX =
    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
private val timeToggleRegex = Regex("($ACTION_DELETE|$ACTION_RESTORE)-($UUID_REGEX)")

fun BehaviourContext.registerLessonChangeDeleteCallback() = onDataCallbackQuery(timeToggleRegex) { query -> db {
    val (action, idStr) = timeToggleRegex.matchEntire(query.data)?.destructured ?: return@onDataCallbackQuery
    changeLessonDelete(
        idStr.toLessonId(),
        action == ACTION_DELETE,
        query
    )
} }

context(_: Session)
private suspend fun BehaviourContext.changeLessonDelete(
    id: LessonId,
    deleted: Boolean,
    query: DataCallbackQuery
) {
    val lessons = LessonMapper.findById(id)
    val lesson = lessons.firstOrNull()
        ?: run {
            answerCallbackQuery(query, "Запись не найдена", showAlert = true)
            return
        }

    if (!data.user.hasAdminPermission() && lesson.tutorId.id != query.userId()) {
        answerCallbackQuery(query, "Доступ запрещён", showAlert = true)
        return
    }
    if (!lesson.dateTime.isInSameMonthAs(LocalDateTime.now())) {
        answerCallbackQuery(query, "Нельзя модифицировать запись после окончания месяца", showAlert = true)
        return
    }

    LessonMapper.setDeleted(id, deleted)
    BotAudit.lesson(query.userId(), if (deleted) "Удалено" else "Восстановлено", id, listOf(Change("удалено", !deleted, deleted)))

    query.messageDataCallbackQueryOrNull()?.message?.let {
        edit(
            it, replyMarkup = buildLessonKeyboard(
                id = id,
                deleted = deleted,
                extraHalfHour = lesson.extraHalfHour,
                forceGroup = lesson.forceGroup,
                isSingle = lessons.size == 1
            )
        )
    }

    if (deleted) {
        answerCallbackQuery(query, "Занятие удалено")
    } else {
        answerCallbackQuery(query, "Занятие восстановлено")
    }


}

private fun buildLessonKeyboard(
    id: LessonId,
    deleted: Boolean,
    extraHalfHour: Boolean,
    forceGroup: Boolean,
    isSingle: Boolean
) = inlineKeyboard {
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
