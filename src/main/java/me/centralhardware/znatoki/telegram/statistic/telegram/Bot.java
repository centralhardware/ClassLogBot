package me.centralhardware.znatoki.telegram.statistic.telegram;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.CallbackHandler;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.*;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final TelegramSender sender;
    private final TelegramUtil telegramUtil;
    private final List<CommandHandler> commandHandlers;
    private final List<CallbackHandler> callbackHandlers;
    private final Redis redis;
    private final InlineHandler inlineHandler;

    private final TimeFsm timeFsm;
    private final PupilFsm pupilFsm;
    private final PaymentFsm paymentFsm;
    private final OrganizationFsm organizationFsm;
    private final InvitationFsm invitationFsm;

    @SneakyThrows
    @PostConstruct
    public void init() {
        sender.setAbsSender(this);
        var commands = SetMyCommands.builder()
                .commands(List.of(createCommand("/addtime", "Добавить запись"),
                        createCommand("/report", "Отчет за текущий месяц"),
                        createCommand("/reportprevious", "Отчет за предыдущий месяц"),
                        createCommand("/reset", "Сбросить состояние")))
                .build();
        execute(commands);
    }

    private BotCommand createCommand(String command, String description){
        return BotCommand
                .builder()
                .command(command)
                .description(description)
                .build();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            telegramUtil.saveStatisticIncome(update);
            telegramUtil.logUpdate(update);

            Long userId = telegramUtil.getUserId(update);

            boolean isStart = Optional.of(update)
                    .map(Update::getMessage)
                    .map(Message::getText)
                    .filter(it -> it.equals("/start") || it.equalsIgnoreCase("/createOrg"))
                    .isPresent();
            if (isStart) {
                processCommand(update);
                return;
            }

            if (!redis.exists(userId.toString()) && !organizationFsm.isActive(userId)){
                sender.sendText("Вам необходимо создать или присоединиться к организации", telegramUtil.getFrom(update));
                return;
            } else if (organizationFsm.isActive(userId)){
                organizationFsm.process(update);
                return;
            }

            if (processCommand(update)) return;

            if (inlineHandler.processInline(update)) return;

            if (processCallback(update)) return;

            if (timeFsm.isActive(update.getMessage().getChatId())) {
                timeFsm.process(update);
                return;
            }

            if(pupilFsm.isActive(update.getMessage().getChatId())){
                pupilFsm.process(update);
                return;
            }

            if(paymentFsm.isActive(update.getMessage().getChatId())){
                paymentFsm.process(update);
                return;
            }

            if (invitationFsm.isActive(userId)){
                invitationFsm.process(update);
                return;
            }

        } catch (Throwable t) {
            log.warn("Error while processing update", t);
        }
    }

    private boolean processCommand(Update update) {
        if (!update.hasMessage()) return false;

        Message message = update.getMessage();

        for (CommandHandler commandHandler : commandHandlers) {
            if (commandHandler.isAcceptable(message)) {
                commandHandler.handle(message);
                return true;
            }
        }

        return false;
    }

    private boolean processCallback(Update update) {
        if (!update.hasCallbackQuery()) return false;

        var callback = update.getCallbackQuery();

        for (var callbackHandler : callbackHandlers) {
            if (callbackHandler.isAcceptable(callback)) {
                callbackHandler.handle(callback);
                return true;
            }
        }

        return false;
    }

    @Getter
    private final String botUsername = Config.getTelegramUsername();

    @Getter
    private final String botToken = Config.getTelegramToken();
}
