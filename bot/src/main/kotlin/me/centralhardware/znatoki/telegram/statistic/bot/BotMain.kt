package me.centralhardware.znatoki.telegram.statistic.bot

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildSubcontextInitialAction
import dev.inmo.tgbotapi.longPolling
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.commands.BotCommandScopeChat
import kotlinx.coroutines.launch
import me.centralhardware.znatoki.telegram.statistic.extensions.*
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.report.dailyReport
import me.centralhardware.znatoki.telegram.statistic.report.monthReport
import me.centralhardware.znatoki.telegram.statistic.runMigrations
import me.centralhardware.znatoki.telegram.statistic.service.StudentService
import me.centralhardware.znatoki.telegram.statistic.telegram.UserExistChecker
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.*
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student.deleteStudentCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student.studentInfoCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.debug.dailyReportCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.resetCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.startCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand.*
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand.addStudentCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand.searchStudentCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand.studentInfoCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.processInline
import restrictAccess

@OptIn(Warning::class)
@Suppress("DeferredResultUnused")
suspend fun main() {
    runMigrations()
    StudentService.init()

    longPolling (
        "ZnatokiStatistic",
        subcontextInitialAction = buildSubcontextInitialAction {
            add { update ->
                runCatching {
                    TutorMapper.findByIdOrNull(update.tutorId())?.let { user -> data.user = user }
                }
            }
        },
        middlewares = {
            addMiddleware { restrictAccess(UserExistChecker()) }
        }
    ) {
        launch {
            KSLog.info("Setting bot commands for all users...")
            TutorMapper.getAll().forEach { user ->
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
                runCatching {
                    setMyCommands(userCommands, scope = BotCommandScopeChat(user.id.toChatId()))
                }.onFailure { KSLog.warning("Failed to set commands for user ${user.id.id}: ${it.message}") }
            }
            KSLog.info("Bot commands set successfully")
        }

        startCommand()

        initContext({ it.hasClientPermission() }) {
            addStudentCommand()
        }

        initContext({ it.hasPaymentPermission() }) {
            addPaymentCommand()
        }

        initContext({ it.canAddPaymentForOthers() }) {
            addPaymentForOtherCommand()
        }

        initContext({ it.canAddTimeForOthers() }) {
            addLessonForOtherCommand()
        }

        initContext({ it.hasTimePermission() }) {
            addLessonCommand()
        }

        initContext({ it.hasReadRight() }) {
            studentInfoCommand()
            searchStudentCommand()
            reportCommand()
            reportPreviousCommand()
            resetCommand()

            studentInfoCallback()

            processInline()

            registerForceGroupHandlers()

            registerExtraHalfHourHandlers()
        }

        initContext({ it.hasAdminPermission() }) {
            dailyReportCommand()
            reportMonthCommand()

            deleteStudentCallback()

            registerLessonChangeDeleteCallback()

            registerPaymentChangeDeleteCallback()
        }

        launch {
            dailyReport()
        }
        launch {
            monthReport()
        }
    }.second.join()
}
