package me.centralhardware.znatoki.telegram.statistic.bot

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.commands.BotCommandScopeChat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.*
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import kotlin.time.Duration.Companion.minutes

private val COMMANDS_REFRESH_INTERVAL = 10.minutes

private fun commandsFor(user: Tutor): List<BotCommand> {
    val userCommands = mutableListOf<BotCommand>()
    if (user.hasTimePermission()) {
        userCommands.apply {
            add(BotCommand("addlesson", "ДОБАВИТЬ ЗАПИСЬ ЗАНЯТИЯ"))
        }
    }
    if (user.hasPaymentPermission()) {
        userCommands.apply {
            add(BotCommand("addpayment", "Ведомость оплаты"))
        }
    }
    if (user.canAddPaymentForOthers()) {
        userCommands.apply {
            add(BotCommand("addpaymentforother", "Добавить оплату за другого репетитора"))
        }
    }
    if (user.canAddTimeForOthers()) {
        userCommands.apply {
            add(BotCommand("addlessonforother", "Добавить занятие за другого репетитора"))
        }
    }
    if (user.hasClientPermission()) {
        userCommands.apply {
            add(BotCommand("addstudent", "Добавить ученика"))
        }
    }
    if (user.hasReadRight()) {
        userCommands.apply {
            add(BotCommand("report", "Отчет за текущий месяц"))
            add(BotCommand("reportprevious", "Отчет за предыдущий месяц"))
            add(BotCommand("web_report", "Веб-отчеты"))
            add(BotCommand("reset", "Сбросить состояние"))
        }
    }
    return userCommands
}

/**
 * Keeps every tutor's command menu in line with their permissions. Permissions can change
 * outside the bot (web app), so the menus are re-checked periodically and only the tutors
 * whose command set changed get a new setMyCommands call.
 */
suspend fun BehaviourContext.syncBotCommands() {
    val applied = mutableMapOf<TutorId, List<BotCommand>>()
    while (isActive) {
        runCatching {
            var updated = 0
            TutorMapper.getAll().forEach { user ->
                val commands = commandsFor(user)
                if (applied[user.id] == commands) return@forEach
                runCatching {
                    setMyCommands(commands, scope = BotCommandScopeChat(user.id.toChatId()))
                    applied[user.id] = commands
                    updated++
                }.onFailure { KSLog.warning("Failed to set commands for user ${user.id.id}: ${it.message}") }
            }
            if (updated > 0) KSLog.info("Bot commands updated for $updated users")
        }.onFailure { KSLog.warning("Bot command sync failed: ${it.message}") }
        delay(COMMANDS_REFRESH_INTERVAL)
    }
}
