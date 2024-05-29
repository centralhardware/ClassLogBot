package me.centralhardware.znatoki.telegram.statistic

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.createSubContextAndDoWithUpdatesFilter
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.types.BotCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.paymentDeleteCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.paymentRestoreCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.timeDeleteCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.timeRestoreCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student.deleteUserCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student.userInfoCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.debug.dailyReportCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.organization.grafanaCommand
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
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("bot")
lateinit var bot: TelegramBot
suspend fun main() {
    val res = telegramBotWithBehaviourAndLongPolling(
        Config.Telegram.token,
        defaultExceptionsHandler = { log.info("", it) },
        scope = CoroutineScope(Dispatchers.IO)
    ) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
        setMyCommands(
            BotCommand("addtime", "Добавить запись"),
            BotCommand("addpayment", "Добавить оплату"),
            BotCommand("report", "Отчет за текущий месяц"),
            BotCommand("reportprevious", "Отчет за предыдущий месяц"),
            BotCommand("reset", "Сбросить состояние")
        )

        onCommand("start") { startCommand(it) }
        onCommand("reset") { resetCommand(it) }

        onContentMessage({ Storage.contain(it.userId()) }) {
            Storage.process(it)
        }



        createSubContextAndDoWithUpdatesFilter(
            stopOnCompletion = false,
            updatesUpstreamFlow = allUpdatesFlow.filter { UserMapper.hasWriteRight(it.userId()) }) {
            onCommand(Regex("addPupil|addpupil")) { addClientCommand(it) }
            onCommand(Regex("addTime|addtime")) { addTimeCommand(it) }
            onCommand(Regex("addPayment|addpayment")) { addPaymentCommand(it) }

            onCommand("report") { reportCommand(it) }
            onCommand(Regex("reportPrevious|reportprevious")) { reportPreviousCommand(it) }
        }

        createSubContextAndDoWithUpdatesFilter(
            stopOnCompletion = false,
            updatesUpstreamFlow = allUpdatesFlow.filter { UserMapper.hasReadRight(it.userId()) }) {
            onCommandWithArgs("i") { message, args -> userInfoCommand(message, args) }
            onCommandWithArgs("s") { message, args -> searchCommand(message, args) }

            onDataCallbackQuery(Regex("\\/user_info\\d+\$")) { userInfoCallback(it) }

            onBaseInlineQuery { processInline(it) }
        }

        createSubContextAndDoWithUpdatesFilter(
            stopOnCompletion = false,
            updatesUpstreamFlow = allUpdatesFlow.filter { UserMapper.isAdmin(it.userId()) }) {
            onCommand("grafana") { grafanaCommand(it) }
            onCommandWithArgs("dailyReport") { message, args -> dailyReportCommand(message, args) }

            onDataCallbackQuery(Regex("\\/delete_user\\d+\$")) { deleteUserCallback(it) }
            onDataCallbackQuery(Regex("timeRestore-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                timeRestoreCallback(it)
            }
            onDataCallbackQuery(Regex("timeDelete-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                timeDeleteCallback(it)
            }
            onDataCallbackQuery(Regex("paymentRestore-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                paymentRestoreCallback(it)
            }
            onDataCallbackQuery(Regex("paymentRestore-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                paymentDeleteCallback(it)
            }
        }

    }
    bot = res.first
    res.second.join()

}