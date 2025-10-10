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
import me.centralhardware.znatoki.telegram.statistic.extensions.hasExtraHalfHour
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

private const val ACTION_ADD = "forceGroupAdd"
private const val ACTION_REMOVE = "forceGroupRemove"
private const val UUID_REGEX =
    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
private val toggleRegex = Regex("($ACTION_ADD|$ACTION_REMOVE)-($UUID_REGEX)")

fun BehaviourContext.registerForceGroupHandlers() = onDataCallbackQuery(toggleRegex) { query ->
    val (action, idStr) = toggleRegex.matchEntire(query.data)!!.destructured
    changeForceGroupStatus(
        idStr.toLessonId(),
        action == ACTION_ADD,
        query)
}

private suspend fun BehaviourContext.changeForceGroupStatus(
    id: LessonId,
    forceGroup: Boolean,
    query: DataCallbackQuery
) {
    val before = LessonMapper.findById(id)
    val service = before.firstOrNull()
        ?: run {
            answerCallbackQuery(query, "Запись не найдена", showAlert = true)
            return
        }

    if (!data.user.hasAdminPermission() && service.tutorId.id != query.userId()) {
        answerCallbackQuery(query, "Доступ запрещён", showAlert = true)
        return
    }
    if (!service.dateTime.isInSameMonthAs(LocalDateTime.now())) {
        answerCallbackQuery(query, "Нельзя модифицировать запись после окончания месяца", showAlert = true)
        return
    }

    if (before.size > 1) {
        answerCallbackQuery(query, "Занятие уже групповое", showAlert = true)
        return
    }

    LessonMapper.setForceGroup(id, forceGroup)

    val after = LessonMapper.findById(id)
    val current = after.firstOrNull() ?: service

    val keyboard = buildServiceKeyboard(
        id = id,
        isDm = query.isDm(),
        canToggleExtraHalfHour = data.user.hasExtraHalfHour(),
        deleted = current.deleted,
        extraHalfHour = current.extraHalfHour,
        showForceGroupSwitcher = after.size == 1,
        forceGroup = current.forceGroup
    )

    query.messageDataCallbackQueryOrNull()
        ?.let { edit(it.message, replyMarkup = keyboard) }
    
    // Audit log
    val htmlDiff = DiffService.generateHtmlDiff(
        oldObj = service,
        newObj = current
    )
    
    val student = StudentMapper.findById(service.studentId)
    val subject = SubjectMapper.getNameById(service.subjectId) ?: "Unknown"
    
    AuditLogMapper.log(
        userId = query.user.id.chatId.long,
        action = "UPDATE_LESSON",
        entityType = "lesson",
        entityId = null,
        details = "<div class=\"entity-info\">${student?.fio()}, $subject</div>$htmlDiff",
        studentId = service.studentId.id,
        subjectId = service.subjectId.id.toInt()
    )
}

private fun buildServiceKeyboard(
    id: LessonId,
    isDm: Boolean,
    canToggleExtraHalfHour: Boolean,
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

    if (canToggleExtraHalfHour) {
        if (extraHalfHour) {
            row { dataButton("Убрать полтора часа", "extraHalfHourRemove-${id.id}") }
        } else {
            row { dataButton("Сделать полтора часа", "extraHalfHourAdd-${id.id}") }
        }
    }

    if (showForceGroupSwitcher) {
        if (forceGroup) {
            row { dataButton("Сделать одиночным занятием", "$ACTION_REMOVE-${id.id}") }
        } else {
            row { dataButton("Сделать групповым занятием", "$ACTION_ADD-${id.id}") }
        }
    }
}
