package me.centralhardware.znatoki.telegram.statistic.telegram;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.Storage;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.Clickhouse;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.minio.Minio;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
import me.centralhardware.znatoki.telegram.statistic.validate.AmountValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.EnumValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.FioValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.PhotoValidator;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final TelegramSender sender;
    private final TelegramUtil telegramUtil;
    private final List<CommandHandler> commandHandlers;
    private final Redis redis;
    private final Minio minio;
    private final Clickhouse clickhouse;
    private final Storage storage;
    private final InlineHandler inlineHandler;

    private final AmountValidator amountValidator;
    private final EnumValidator enumValidator;
    private final PhotoValidator photoValidator;
    private final FioValidator fioValidator;

    @PostConstruct
    public void init(){
        sender.setAbsSender(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            telegramUtil.saveStatisticIncome(update);
            telegramUtil.logUpdate(update);

            Long userId = telegramUtil.getUserId(update);
            if (!redis.exists(userId.toString()) &&
                    !userId.equals(Long.parseLong(System.getenv("ADMIN_ID")))){

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

            if (storage.contain(update.getMessage().getChatId())){
                completeTime(update);
            }
        } catch (Throwable t){
            log.warn("Error while processing update",t);
        }
    }

    private boolean processCommand(Update update) {
        if (!update.hasMessage()) return false;

        Message message = update.getMessage();

        for (CommandHandler commandHandler : commandHandlers){
            if (commandHandler.isAcceptable(message)){
                commandHandler.handle(message);
                return true;
            }
        }

        return false;
    }

    private void completeTime(Update update){
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

                if (!Objects.equals(text, "/complete")){
                    var fioRes = fioValidator.validate(text);

                    if (fioRes.isLeft()){
                        sender.sendText(fioRes.getLeft(), user);
                        return;
                    }
                    storage.getTIme(userId).getFios().add(text);
                    sender.sendText("ФИО сохранено", user);
                    return;
                }

                if (storage.getTIme(userId).getFios().isEmpty()){
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

                if (res.isLeft()){
                    sender.sendText(res.getLeft(), user);
                    return;
                }

                GetFile getFile = new GetFile();
                getFile.setFileId(res.right().get().getFileId());
                try {
                    File file = downloadFile(execute(getFile));

                    String id = minio.upload(file.getAbsolutePath());
                    storage.getTIme(userId).setPhotoId(id);

                    Time time = storage.getTIme(userId);
                    time.getFios().forEach(it -> {
                        time.setFio(it);
                        clickhouse.insert(time);
                    });

                    storage.remove(userId);

                    sender.sendText("Сохранено", user);

                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Getter
    private final String botUsername = System.getenv("BOT_USERNAME");

    @Getter
    private final String botToken = System.getenv("BOT_TOKEN");
}
