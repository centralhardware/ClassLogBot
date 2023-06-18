package me.centralhardware.znatoki.telegram.statistic.telegram;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.Files;
import me.centralhardware.znatoki.telegram.statistic.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.Clickhouse;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.Subjects;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.Time;
import me.centralhardware.znatoki.telegram.statistic.handler.CallbackHandler;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.ZnatokiUser;
import one.util.streamex.StreamEx;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final TelegramSender sender;
    private final TelegramUtil telegramUtil;
    private final List<CallbackHandler> callbackHandlers;
    private final Redis redis;
    private final Files files;
    private final Clickhouse clickhouse;

    @PostConstruct
    public void init(){
        sender.setAbsSender(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            telegramUtil.saveStatisticIncome(update);

            if(sendStartMessage(update)) return;

            Long userId = telegramUtil.getUserId(update);
            if (!redis.exists(userId.toString()) && !userId.equals(Long.parseLong(System.getenv("ADMIN_ID")))){
                SendMessage message = SendMessage.builder()
                        .text("Доступ запрещен")
                        .chatId(userId)
                        .build();
                return;
            }

            if (processAddTimeCommand(update)) return;
            if (processRegisterCommand(update)) return;

            if (fsm.containsKey(update.getMessage().getChatId())){
                completeTime(update);
            }

            processCallback(update);
        } catch (Throwable t){
            log.warn("Error while processing update",t);
        }
    }

    private void processCallback(Update update) {
        if (!update.hasCallbackQuery()) return;

        var callbackQuery = update.getCallbackQuery();

        StreamEx.of(callbackHandlers)
                .filter(it -> it.isAcceptable(callbackQuery))
                .forEach(it -> it.handle(callbackQuery));
    }

    private final Map<Long, Time> fsm = new HashMap<>();
    private final Map<Long, Integer> fsmStage = new HashMap<>();

    private void completeTime(Update update){
        Long userId = telegramUtil.getUserId(update);
        String text = Optional.of(update)
                .map(Update::getMessage)
                .map(Message::getText)
                .orElse(null);

        switch (fsmStage.get(userId)){
            case 1: {
                fsm.get(userId).setSubject(Subjects.of(text).name());
                SendMessage message = SendMessage.builder()
                        .chatId(userId)
                        .text("Введите фио")
                        .build();
                sender.send(message, update.getMessage().getFrom());
                fsmStage.put(userId, 2);
                break;
            }
            case 2: {
                fsm.get(userId).setFio(text);
                SendMessage message = SendMessage.builder()
                        .chatId(userId)
                        .text("Введите сумму")
                        .build();
                sender.send(message, update.getMessage().getFrom());
                fsmStage.put(userId, 3);
                break;
            }
            case 3: {
                fsm.get(userId).setAmount(Integer. parseInt(text));
                SendMessage message = SendMessage.builder()
                        .chatId(userId)
                        .text("Отправьте фото отчётностии")
                        .build();
                sender.send(message, update.getMessage().getFrom());
                fsmStage.put(userId, 4);
                break;
            }
            case 4: {
                if (!update.getMessage().hasPhoto()){
                    SendMessage message = SendMessage.builder()
                            .chatId(userId)
                            .text("Отправьте фото")
                            .build();
                    sender.send(message, update.getMessage().getFrom());
                    return;
                }

                PhotoSize photoSize = update.getMessage().getPhoto().get(0);

                GetFile getFile = new GetFile();
                getFile.setFileId(photoSize.getFileId());
                try {
                    File file = downloadFile(execute(getFile));

                    String id = files.upload(file.getAbsolutePath());
                    fsm.get(userId).setPhotoId(id);

                    fsm.get(userId).setPhotoId(id);

                    clickhouse.insert(fsm.get(userId));
                    fsm.remove(userId);
                    fsmStage.remove(userId);

                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private boolean processAddTimeCommand(Update update){
        if (!update.hasMessage() || (update.getMessage().getText() == null || !update.getMessage().getText().startsWith("/addTime"))) return false;

        ZnatokiUser user = redis.get(update.getMessage().getChatId().toString(), ZnatokiUser.class);

        Time time = new Time();
        time.setDateTime(LocalDateTime.now());
        time.setChatId(update.getMessage().getChatId());

        if (user.subjects().size() == 1){
            time.setSubject(user.subjects().get(0).toString());
        }

        fsm.put(update.getMessage().getChatId(), time);

        if (user.subjects().size() != 1){
            ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create();
            builder.setText("Выберите предмет");

            user.subjects().forEach(it -> builder.row().button(it.getRusName()).endRow());
            sender.send(builder.build(update.getMessage().getChatId()), update.getMessage().getFrom());
            fsmStage.put(update.getMessage().getChatId(), 1);
            return true;
        }{
            SendMessage message = SendMessage.builder()
                    .chatId(update.getMessage().getChatId())
                    .text("Введите фио")
                    .build();
            sender.send(message, update.getMessage().getFrom());
            fsmStage.put(update.getMessage().getChatId(), 2);
            return true;
        }
    }

    public boolean processRegisterCommand(Update update){
        if (!update.hasMessage() || update.getMessage().getText() == null || !update.getMessage().getText().startsWith("/register")) return false;

        if (!update.getMessage().getFrom().getId().equals(Long.parseLong(System.getenv("ADMIN_ID")))){
            return true;
        }

        String messasge = update.getMessage().getText();

        String chatId = messasge.split(" ")[1];
        List<Subjects> subjects = Arrays.stream(messasge.replace("/register " + chatId + " ", "")
                        .split(" "))
                .map(Subjects::valueOf)
                .toList();

        ZnatokiUser user = ZnatokiUser.builder()
                .subjects(subjects)
                .build();

        redis.put(chatId, user);

        return true;
    }

    /**
     * Process /start command
     * @return True, if handled
     */
    private boolean sendStartMessage(Update update) {
        if (!update.hasMessage() || !Objects.equals(update.getMessage().getText(), "/start")) return false;

        var message = SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text("""
                        Бот для контроля учебных часов в Знатоках.
                        Автор: @centralhardware
                        """)
                .build();

        sender.send(message, update.getMessage().getFrom());

        return true;
    }

    @Getter
    private final String botUsername = System.getenv("BOT_USERNAME");

    @Getter
    private final String botToken = System.getenv("BOT_TOKEN");
}
