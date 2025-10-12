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
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.entity.toLessonId
import me.centralhardware.znatoki.telegram.statistic.extensions.hasAdminPermission
import me.centralhardware.znatoki.telegram.statistic.extensions.hasForceGroup
import me.centralhardware.znatoki.telegram.statistic.extensions.isDm
import me.centralhardware.znatoki.telegram.statistic.extensions.isInSameMonthAs
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.service.DiffService
import me.centralhardware.znatoki.telegram.statistic.user
import java.time.LocalDateTime

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
    changeExtraHalfHour(
        idStr.toLessonId(),
        action == ACTION_ADD,
        query
    )
}

private suspend fun BehaviourContext.changeExtraHalfHour(
    id: LessonId,
    extraHalfHour: Boolean,
    query: DataCallbackQuery
) {
    val chatId = query.from.id.chatId.long
    val historyBefore = LessonMapper.findById(id)
    val service = historyBefore.firstOrNull()
        ?: run {
            answerCallbackQuery(query, "Запись не найдена", showAlert = true)
            return
        }

    if (!data.user.hasAdminPermission() && service.tutorId.id != chatId) {
        answerCallbackQuery(query, "Доступ запрещён", showAlert = true)
        return
    }
    if (!service.dateTime.isInSameMonthAs(LocalDateTime.now())) {
        answerCallbackQuery(query, "Нельзя модифицировать запись после окончания месяца", showAlert = true)
        return
    }

    LessonMapper.setExtraHalfHour(id, extraHalfHour)

    val historyAfter = LessonMapper.findById(id)
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
    
    val htmlDiff = DiffService.generateHtmlDiff(
        oldObj = service,
        newObj = updated
    )
    
    val student = StudentMapper.findById(service.studentId)
    val subject = SubjectMapper.getNameById(service.subjectId) ?: "Unknown"
    
    AuditLogMapper.log(
        userId = query.user.id.chatId.long,
        action = "UPDATE_LESSON",
        entityType = "lesson",
        entityId = service.id.id.toString(),
        studentId = service.studentId.id,
        subjectId = service.subjectId.id.toInt(),
        oldEntity = service,
        newEntity = updated
    )
}

private fun buildServiceKeyboard(
    id: LessonId,
    isDm: Boolean,
    canForceGroup: Boolean,
    deleted: Boolean,
    extraHalfHour: Boolean,
    showForceGroupSwitcher: Boolean,
    forceGroup: Boolean
) = inlineKeyboard {
    if (!isDm) {
        if (deleted) {
            row { dataButton("Восстановить", "timeRestore-${id.id}") }
        } else {
            row { dataButton("Удалить", "timeDelete-${id.id}") }
        }
    }

    if (extraHalfHour) {
        row { dataButton("Убрать полтора часа", "$ACTION_REMOVE-${id.id}") }
    } else {
        row { dataButton("Сделать полтора часа", "$ACTION_ADD-${id.id}") }
    }

    if (showForceGroupSwitcher && canForceGroup) {
        if (forceGroup) {
            row { dataButton("Сделать одиночным занятием", "forceGroupRemove-${id.id}") }
        } else {
            row { dataButton("Сделать групповым занятием", "forceGroupAdd-${id.id}") }
        }
    }
}
