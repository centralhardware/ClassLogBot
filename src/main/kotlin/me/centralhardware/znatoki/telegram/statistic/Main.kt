package me.centralhardware.znatoki.telegram.statistic

import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.createSubContextAndDoAsynchronouslyWithUpdatesFilter
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.longPolling
import dev.inmo.tgbotapi.types.BotCommand
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.report.dailyReport
import me.centralhardware.znatoki.telegram.statistic.report.monthReport
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.paymentDeleteCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.paymentRestoreCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.timeDeleteCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.timeRestoreCallback
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

lateinit var bot: TelegramBot

@OptIn(Warning::class)
@Suppress("DeferredResultUnused")
suspend fun main() {
    AppConfig.init("ZnatokiStatistic")
    ClientService.init()
    bot =
        longPolling {
                setMyCommands(
                    BotCommand("addtime", "ДОБАВИТЬ ЗАПИСЬ ЗАНЯТИЯ"),
                    BotCommand("report", "Отчет за текущий месяц"),
                    BotCommand("reportprevious", "Отчет за предыдущий месяц"),
                    BotCommand("addpayment", "Ведомость оплаты"),
                    BotCommand("reset", "Сбросить состояние"),
                )

                onCommand("start") { startCommand(it) }
                onCommand("reset") { resetCommand(it) }

                onContentMessage({ Storage.contain(it.userId()) }) { Storage.process(it) }

                createSubContextAndDoAsynchronouslyWithUpdatesFilter(
                    updatesUpstreamFlow =
                        allUpdatesFlow.filter { UserMapper.hasClientPermission(it.userId()) }
                ) {
                    onCommand(Regex("addPupil|addpupil")) { addClientCommand(it) }
                }

                createSubContextAndDoAsynchronouslyWithUpdatesFilter(
                    updatesUpstreamFlow =
                        allUpdatesFlow.filter { UserMapper.hasPaymentPermission(it.userId()) }
                ) {
                    onCommand(Regex("addPayment|addpayment")) { addPaymentCommand(it) }
                }

                createSubContextAndDoAsynchronouslyWithUpdatesFilter(
                    updatesUpstreamFlow =
                        allUpdatesFlow.filter { UserMapper.hasTimePermission(it.userId()) }
                ) {
                    onCommand(Regex("addTime|addtime")) { addTimeCommand(it) }
                }

                createSubContextAndDoAsynchronouslyWithUpdatesFilter(
                    updatesUpstreamFlow =
                        allUpdatesFlow.filter { UserMapper.hasReadRight(it.userId()) }
                ) {
                    onCommandWithArgs("i") { message, args -> userInfoCommand(message, args) }
                    onCommandWithArgs("s") { message, args -> searchCommand(message, args) }
                    onCommand("report") { reportCommand(it) }
                    onCommand(Regex("reportPrevious|reportprevious")) { reportPreviousCommand(it) }

                    onDataCallbackQuery(Regex("user_info\\d+\$")) { userInfoCallback(it) }

                    onBaseInlineQuery { processInline(it) }
                }

                createSubContextAndDoAsynchronouslyWithUpdatesFilter(
                    updatesUpstreamFlow =
                        allUpdatesFlow.filter { UserMapper.hasAdminPermission(it.userId()) }
                ) {
                    onCommandWithArgs("dailyReport") { message, args ->
                        dailyReportCommand(message, args)
                    }

                    onDataCallbackQuery(Regex("delete_user\\d+\$")) { deleteUserCallback(it) }
                    onDataCallbackQuery(
                        Regex(
                            "timeRestore-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                        )
                    ) {
                        timeRestoreCallback(it)
                    }
                    onDataCallbackQuery(
                        Regex(
                            "timeDelete-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                        )
                    ) {
                        timeDeleteCallback(it)
                    }
                    onDataCallbackQuery(Regex("paymentRestore-\\d+\$")) {
                        paymentRestoreCallback(it)
                    }
                    onDataCallbackQuery(Regex("paymentDelete-\\d+\$")) { paymentDeleteCallback(it) }
                }
            }
            .first
    coroutineScope {
        launch { monthReport() }
        launch { dailyReport() }
    }
}
