package me.centralhardware.znatoki.telegram.statistic

import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.createSubContextAndDoAsynchronouslyWithUpdatesFilter
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.longPolling
import dev.inmo.tgbotapi.types.BotCommand
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.report.dailyReport
import me.centralhardware.znatoki.telegram.statistic.report.monthReport
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.*
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student.deleteUserCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student.userInfoCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.debug.dailyReportCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.resetCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.startCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand.addPaymentCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand.addTimeCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand.reportCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand.reportPreviousCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand.addClientCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand.searchCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand.userInfoCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.processInline

@OptIn(Warning::class)
@Suppress("DeferredResultUnused")
suspend fun main() {
    embeddedServer(Netty, port = 999) {
        module()
    }.start(wait = false)
    AppConfig.init("ZnatokiStatistic")
    ClientService.init()
    longPolling {
        setMyCommands(
            BotCommand("addtime", "ДОБАВИТЬ ЗАПИСЬ ЗАНЯТИЯ"),
            BotCommand("report", "Отчет за текущий месяц"),
            BotCommand("reportprevious", "Отчет за предыдущий месяц"),
            BotCommand("addpayment", "Ведомость оплаты"),
            BotCommand("reset", "Сбросить состояние"),
        )

        startCommand()
        resetCommand()

        onContentMessage({ Storage.contain(it.userId()) }) { Storage.process(it) }

        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow =
                allUpdatesFlow.filter { UserMapper.hasClientPermission(it.userId()) }
        ) {
            addClientCommand()
        }

        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow =
                allUpdatesFlow.filter { UserMapper.hasPaymentPermission(it.userId()) }
        ) {
            addPaymentCommand()
        }

        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow =
                allUpdatesFlow.filter { UserMapper.hasTimePermission(it.userId()) }
        ) {
            addTimeCommand()
        }

        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow =
                allUpdatesFlow.filter { UserMapper.hasReadRight(it.userId()) }
        ) {
            userInfoCommand()
            searchCommand()
            reportCommand()
            reportPreviousCommand()

            userInfoCallback()

            processInline()

            forceGroupAdd()
            forceGroupRemove()
        }

        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow =
                allUpdatesFlow.filter { UserMapper.hasAdminPermission(it.userId()) }
        ) {
            dailyReportCommand()

            deleteUserCallback()

            timeRestoreCallback()
            timeDeleteCallback()

            paymentRestoreCallback()
            paymentDeleteCallback()
        }

        launch {
            monthReport()
        }
        launch {
            dailyReport()
        }
    }.second.join()
}
