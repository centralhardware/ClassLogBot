package me.centralhardware.znatoki.telegram.statistic.telegram;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.telegram.bot.common.ClickhouseRuben;
import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.TelegramUser;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.CallbackHandler;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.ClientFsm;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.PaymentFsm;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.TimeFsm;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class Bot implements SpringLongPollingBot {

    private final ClickhouseRuben clickhouse = new ClickhouseRuben();

    private final TelegramSender sender;
    private final TelegramUtil telegramUtil;
    private final List<CommandHandler> commandHandlers;
    private final List<CallbackHandler> callbackHandlers;
    private final InlineHandler inlineHandler;

    private final TimeFsm timeFsm;
    private final ClientFsm clientFsm;
    private final PaymentFsm paymentFsm;

    private final UserMapper userMapper;
    private final OkHttpTelegramClient telegramClient;

    @SneakyThrows
    @PostConstruct
    public void init() {
        var commands = SetMyCommands.builder()
                .commands(List.of(createCommand("/addtime", "Добавить запись"),
                        createCommand("/addpayment", "Добавить оплату"),
                        createCommand("/report", "Отчет за текущий месяц"),
                        createCommand("/reportprevious", "Отчет за предыдущий месяц"),
                        createCommand("/reset", "Сбросить состояние")))
                .build();
        telegramClient.execute(commands);
    }

    private BotCommand createCommand(String command, String description){
        return BotCommand
                .builder()
                .command(command)
                .description(description)
                .build();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updates -> updates.forEach(this::onUpdateReceived);
    }

    public void onUpdateReceived(Update update) {
        try {
            clickhouse.log(update, "znatokiStatistic");
            telegramUtil.logUpdate(update);

            Long userId = telegramUtil.getUserId(update);

            TelegramUser user = userMapper.getById(userId);

            if (user != null && user.getRole() == Role.BLOCK){
                log.info("Access blocked for user {}({})", user.getName(), user.getId());
                return;
            }

            boolean isStart = Optional.of(update)
                    .map(Update::getMessage)
                    .map(Message::getText)
                    .filter(it -> it.equals("/start"))
                    .isPresent();
            if (isStart) {
                processCommand(update);
                return;
            }

            if (userMapper.getById(userId) == null){
                sender.sendText("Вам необходимо создать или присоединиться к организации", TelegramUtil.getFrom(update));
                return;
            }

            if (processCommand(update)) return;

            if (inlineHandler.processInline(update)) return;

            if (processCallback(update)) return;

            if (timeFsm.isActive(userId)) {
                timeFsm.process(update);
                return;
            }

            if(clientFsm.isActive(userId)){
                clientFsm.process(update);
                return;
            }

            if(paymentFsm.isActive(userId)){
                paymentFsm.process(update);
                return;
            }

        } catch (Throwable t) {
            log.warn("Error while processing update", t);
        }
    }

    private boolean processCommand(Update update) {
        return processHandler(update, commandHandlers);
    }

    private boolean processCallback(Update update) {
        return processHandler(update, callbackHandlers);
    }

    private <T extends Handler> boolean processHandler(Update update, List<T> handlers){
        for (var handler : handlers) {
            if (handler.isAcceptable(update)) {
                handler.handle(update);
                return true;
            }
        }

        return false;
    }

    @Getter
    private final String botToken = Config.Telegram.getTelegramToken();

}
