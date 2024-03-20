package me.centralhardware.znatoki.telegram.statistic.telegram

import jakarta.annotation.PostConstruct
import me.centralhardware.telegram.bot.common.ClickhouseRuben
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.CallbackHandler
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.CommandHandler
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.userId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand

@Component
class Bot(
    private val sender: TelegramSender,
    private val commandHandlers: List<CommandHandler>,
    private val callbackHandlers: List<CallbackHandler>,
    private val inlineHandler: InlineHandler,
    private val userMapper: UserMapper,
    private val telegramClient: OkHttpTelegramClient,
    private val storage: Storage
) : SpringLongPollingBot {

    private val clickhouse = ClickhouseRuben()
    private val log: Logger = LoggerFactory.getLogger(Bot::class.java)

    @PostConstruct
    fun init() {
        val commands = SetMyCommands.builder()
            .commands(listOf(
                createCommand("/addtime", "Добавить запись"),
                createCommand("/addpayment", "Добавить оплату"),
                createCommand("/report", "Отчет за текущий месяц"),
                createCommand("/reportprevious", "Отчет за предыдущий месяц"),
                createCommand("/reset", "Сбросить состояние")))
            .build()
        telegramClient.execute(commands)
    }

    private fun createCommand(command: String, description: String): BotCommand =
        BotCommand.builder()
            .command(command)
            .description(description)
            .build()

    override fun getBotToken(): String = Config.Telegram.token

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer =
        LongPollingUpdateConsumer { updates -> updates.forEach { onUpdateReceived(it) } }

    private fun onUpdateReceived(update: Update) {
        try {
            clickhouse.log(update, "znatokiStatistic")

            val userId = update.userId()

            val user = userMapper.getById(userId)

            user?.let {
                if (it.role == Role.BLOCK){
                    log.info("Access blocked for user ${user.name}(${user.id})")
                    return
                }
            }

            val isStart = update.message?.takeIf { it.text == "/start" } != null
            if (isStart) {
                processCommand(update)
                return
            }

            if (userMapper.getById(userId) == null){
                sender.sendText("Вам необходимо создать или присоединиться к организации", userId)
                return
            }

            if (processCommand(update)) return

            if (inlineHandler.processInline(update)) return

            if (processCallback(update)) return

            storage.takeIf { it.contain(userId) }?.process(userId, update)

        } catch (t: Throwable) {
            log.warn("Error while processing update", t)
        }
    }

    private fun processCommand(update: Update) = processHandler(update, commandHandlers)

    private fun processCallback(update: Update) = processHandler(update, callbackHandlers)

    private fun <T : Handler> processHandler(update: Update, handlers: List<T>): Boolean =
        handlers.find { handler -> handler.isAcceptable(update) }?.let {
            it.handle(update)
            true
        } ?: false
}