package me.centralhardware.znatoki.telegram.statistic

import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextData
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildSubcontextInitialAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.createSubContextAndDoAsynchronouslyWithUpdatesFilter
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.longPolling
import dev.inmo.tgbotapi.types.BotCommand
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser
import me.centralhardware.znatoki.telegram.statistic.extensions.hasAdminPermission
import me.centralhardware.znatoki.telegram.statistic.extensions.hasClientPermission
import me.centralhardware.znatoki.telegram.statistic.extensions.hasPaymentPermission
import me.centralhardware.znatoki.telegram.statistic.extensions.hasReadRight
import me.centralhardware.znatoki.telegram.statistic.extensions.hasTimePermission
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

val defaultCommands = mutableListOf(
    BotCommand("addtime", "ДОБАВИТЬ ЗАПИСЬ ЗАНЯТИЯ"),
    BotCommand("report", "Отчет за текущий месяц"),
    BotCommand("reportprevious", "Отчет за предыдущий месяц"),
    BotCommand("reset", "Сбросить состояние")
)

var BehaviourContextData.user: TelegramUser
    get() = get("user") as TelegramUser
    set(value) = set("user", value)

@OptIn(Warning::class)
@Suppress("DeferredResultUnused")
suspend fun main() {
    embeddedServer(Netty, port = 999) {
        minioProxy()
    }.start(wait = false)
    AppConfig.init("ZnatokiStatistic")
    ClientService.init()
    longPolling(
        subcontextInitialAction = buildSubcontextInitialAction {
            add { update ->
                runCatching {
                    UserMapper.findById(update.userId())?.let { user -> data.user = user }
                }
            }
        }
    ) {
        UserMapper.getAll().forEach { user ->
            setMyCommands(
                when {
                    user.hasPaymentPermission() -> {
                        defaultCommands.apply {
                            add(BotCommand("addpayment", "Ведомость оплаты"))
                        }
                    }

                    user.hasClientPermission() -> {
                        defaultCommands.apply {
                            add(BotCommand("addpupil", "Добавить ученика"))
                        }
                    }

                    user.hasAdminPermission() -> {
                        defaultCommands.apply {
                            add(BotCommand("addpayment", "Ведомость оплаты"))
                            add(BotCommand("addpupil", "Добавить ученика"))
                        }
                    }

                    else -> defaultCommands
                }
            )
        }

        startCommand()
        resetCommand()

        onContentMessage({ Storage.contain(it.userId()) }) { Storage.process(it) }

        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow =
                allUpdatesFlow.filter {
                    return@filter runCatching {
                        UserMapper.findById(it.userId()).hasClientPermission()
                    }.getOrDefault(false)
                }
        ) {
            addClientCommand()
        }

        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow =
                allUpdatesFlow.filter {
                    return@filter runCatching {
                        UserMapper.findById(it.userId()).hasPaymentPermission()
                    }.getOrDefault(false)
                }
        ) {
            addPaymentCommand()
        }

        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow =
                allUpdatesFlow.filter {
                    return@filter runCatching {
                        UserMapper.findById(it.userId()).hasTimePermission()
                    }.getOrDefault(false)
                }
        ) {
            addTimeCommand()
        }

        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow =
                allUpdatesFlow.filter {
                    return@filter runCatching {
                        UserMapper.findById(it.userId()).hasReadRight()
                    }.getOrDefault(false)
                }
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
                allUpdatesFlow.filter {
                    return@filter runCatching {
                        UserMapper.findById(it.userId()).hasAdminPermission()
                    }.getOrDefault(false)
                }
        ) {
            dailyReportCommand()

            deleteUserCallback()

            timeRestoreCallback()
            timeDeleteCallback()

            paymentRestoreCallback()
            paymentDeleteCallback()
        }

        launch {
            dailyReport()
        }
        launch {
            monthReport()
        }
    }.second.join()
}
