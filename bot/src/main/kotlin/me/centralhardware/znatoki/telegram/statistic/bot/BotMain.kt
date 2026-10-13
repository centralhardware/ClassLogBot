package me.centralhardware.znatoki.telegram.statistic.bot

import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildSubcontextInitialAction
import dev.inmo.tgbotapi.longPolling
import dev.inmo.tgbotapi.types.BotCommand
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
            syncBotCommands()
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
