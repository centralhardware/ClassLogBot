package me.centralhardware.znatoki.telegram.statistic.telegram;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.Storage;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.mapper.TeacherNameMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.minio.Minio;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
import me.centralhardware.znatoki.telegram.statistic.validate.AmountValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.EnumValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.FioValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.PhotoValidator;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final TelegramSender sender;
    private final TelegramUtil telegramUtil;
    private final List<CommandHandler> commandHandlers;
    private final Redis redis;
    private final Minio minio;
    private final Storage storage;
    private final InlineHandler inlineHandler;
    private final TimeMapper timeMapper;
    private final TeacherNameMapper teacherNameMapper;

    private final AmountValidator amountValidator;
    private final EnumValidator enumValidator;
    private final PhotoValidator photoValidator;
    private final FioValidator fioValidator;

    @SneakyThrows
    @PostConstruct
    public void init() {
        sender.setAbsSender(this);
        var commands = SetMyCommands.builder()
                .command(BotCommand
                        .builder()
                        .command("/addtime")
                        .description("Добавить запись")
                        .build())
                .build();
        execute(commands);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            telegramUtil.saveStatisticIncome(update);
            telegramUtil.logUpdate(update);

            Long userId = telegramUtil.getUserId(update);
            if (!redis.exists(userId.toString()) &&
                    !userId.equals(Long.parseLong(System.getenv("ADMIN_ID")))) {

                Boolean isStart = Optional.of(update)
                        .map(Update::getMessage)
                        .map(Message::getText)
                        .filter(it -> it.equals("/start"))
                        .isPresent();
                if (isStart) processCommand(update);

                sender.sendText("Доступ запрещен", telegramUtil.getFrom(update));
                return;
            }

            if (processCommand(update)) return;

            if (inlineHandler.processInline(update)) return;

            if (storage.contain(update.getMessage().getChatId())) {
                completeTime(update);
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

    private void completeTime(Update update) {
        Long userId = telegramUtil.getUserId(update);
        String text = Optional.of(update)
                .map(Update::getMessage)
                .map(Message::getText)
                .orElse(null);

        User user = telegramUtil.getFrom(update);
        switch (storage.getStage(userId)) {
            case 1 -> {

                var res = enumValidator.validate(text);

                if (res.isLeft()) {
                    sender.sendText(res.getLeft(), user, false);
                    return;
                }

                storage.getTIme(userId).setSubject(res.right().get().name());
                sender.sendText("Введите фио. /complete - для окончания ввода", user);
                InlineKeyboardBuilder builder = InlineKeyboardBuilder.create()
                        .row().switchToInline().endRow();
                builder.setText("нажмите для поиска фио");
                sender.send(builder.build(userId), update.getMessage().getFrom());
                storage.setStage(userId, 2);
            }
            case 2 -> {

                if (!Objects.equals(text, "/complete")) {
                    var fioRes = fioValidator.validate(text);

                    if (fioRes.isLeft()) {
                        sender.sendText(fioRes.getLeft(), user);
                        return;
                    }

                    if (storage.getTIme(userId).getFios().contains(text)){
                        sender.sendText("Данное ФИО уже добавлено", user);
                        return;
                    }

                    storage.getTIme(userId).getFios().add(text);
                    sender.sendText("ФИО сохранено", user);
                    return;
                }

                if (storage.getTIme(userId).getFios().isEmpty()) {
                    sender.sendText("Необходимо ввести как минимум одно ФИО", user);
                    return;
                }

                storage.getTIme(userId).setFio(text);
                sender.sendText("Введите стоимость занятия", user);
                storage.setStage(userId, 3);
            }
            case 3 -> {
                var res = amountValidator.validate(text);
                if (res.isLeft()) {
                    sender.sendText(res.getLeft(), user);
                    return;
                }

                storage.getTIme(userId).setAmount(res.right().get());
                sender.sendText("Отправьте фото отчётностии", user);
                storage.setStage(userId, 4);
            }
            case 4 -> {
                var res = photoValidator.validate(update);

                if (res.isLeft()) {
                    sender.sendText(res.getLeft(), user);
                    return;
                }

                GetFile getFile = new GetFile();
                getFile.setFileId(res.right().get().getFileId());
                try {
                    Time time = storage.getTIme(userId);

                    File file = downloadFile(execute(getFile));

                    String id = minio.upload(file, time.getDateTime(), teacherNameMapper.getFio(time.getChatId()), time.getSubject());
                    storage.getTIme(userId).setPhotoId(id);

                    ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create()
                            .setText(String.format("""
                                            Предмет: %s,
                                            ФИО: %s
                                            стоимость занятия: %s 
                                            """,
                                    Subject.valueOf(time.getSubject()).getRusName(),
                                    String.join(";", time.getFios()),
                                    time.getAmount().toString()))
                            .row().button("да").endRow()
                            .row().button("нет").endRow();

                    sender.send(builder.build(userId), user);
                    storage.setStage(userId, 5);

                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
            case 5 -> {
                if (text.equals("да")) {
                    Time time = storage.getTIme(userId);
                    var id = UUID.randomUUID();
                    time.getFios().forEach(it -> {
                        time.setFio(it);
                        time.setId(id);
                        timeMapper.insertTime(time);
                    });

                    var logUser = new User();
                    logUser.setId(Long.parseLong(System.getenv("LOG_CHAT")));
                    logUser.setUserName("logger");
                    logUser.setLanguageCode("ru");
                    sender.sendText(String.format("""
                                            Время: %s,
                                            Предмет: %s
                                            Ученики: %s
                                            Стоимость: %s,
                                            Преподаватель: %s
                                            """,
                                    time.getDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm")),
                                    "#" + Subject.valueOf(time.getSubject()).getRusName().replaceAll(" ", "_"),
                                    time.getFios().stream()
                                            .map(it -> "#" + it.replaceAll(" ", "_"))
                                            .collect(Collectors.joining(", ")),
                                    time.getAmount(),
                                    "#" + teacherNameMapper.getFio(userId).replaceAll(" ", "_")),
                            logUser);
                    SendPhoto sendPhoto = SendPhoto
                            .builder()
                            .photo(new InputFile(minio.get(time.getPhotoId()), "отчет"))
                            .chatId(logUser.getId())
                            .build();
                    sender.send(sendPhoto, logUser);

                    storage.remove(userId);

                    sender.sendText("Сохранено", user, true);
                } else if (text.equals("нет")) {
                    minio.delete(storage.getTIme(userId).getPhotoId());
                    storage.remove(userId);
                    sender.sendText("Удалено", user, true);
                }
            }
        }
    }

    @Getter
    private final String botUsername = System.getenv("BOT_USERNAME");

    @Getter
    private final String botToken = System.getenv("BOT_TOKEN");
}
