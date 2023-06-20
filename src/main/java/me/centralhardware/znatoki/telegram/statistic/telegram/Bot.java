package me.centralhardware.znatoki.telegram.statistic.telegram;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.Clickhouse;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.handler.CallbackHandler;
import me.centralhardware.znatoki.telegram.statistic.lucen.Lucene;
import me.centralhardware.znatoki.telegram.statistic.minio.Minio;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.validate.AmountValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.EnumValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.FioValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.PhotoValidator;
import one.util.streamex.StreamEx;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final TelegramSender sender;
    private final TelegramUtil telegramUtil;
    private final List<CallbackHandler> callbackHandlers;
    private final Redis redis;
    private final Minio minio;
    private final Clickhouse clickhouse;
    private final Lucene lucene;

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

            if(sendStartMessage(update)) return;

            Long userId = telegramUtil.getUserId(update);
            if (!redis.exists(userId.toString()) && !userId.equals(Long.parseLong(System.getenv("ADMIN_ID")))){
                sender.sendText("Доступ запрещен", update);
                return;
            }

            if (processInline(update)) return;
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

        switch (fsmStage.get(userId)) {
            case 1 -> {

                var res = enumValidator.validate(text);

                if (res.isLeft()) {
                    sender.sendText(res.getLeft(), update, false);
                    return;
                }

                fsm.get(userId).setSubject(res.right().get().name());
                sender.sendText("Введите фио. /complete - для окончания ввода", update);
                InlineKeyboardBuilder builder = InlineKeyboardBuilder.create()
                                .row().switchToInline().endRow();
                builder.setText("нажмите для поиска фио");
                sender.send(builder.build(userId), update.getMessage().getFrom());
                fsmStage.put(userId, 2);
            }
            case 2 -> {

                if (!Objects.equals(text, "/complete")){
                    var fioRes = fioValidator.validate(text);

                    if (fioRes.isLeft()){
                        sender.sendText(fioRes.getLeft(), update);
                        return;
                    }
                    fsm.get(userId).getFios().add(text);
                    sender.sendText("ФИО сохранено", update);
                    return;
                }

                fsm.get(userId).setFio(text);
                sender.sendText("Введите сумму", update);
                fsmStage.put(userId, 3);
            }
            case 3 -> {
                var res = amountValidator.validate(text);
                if (res.isLeft()) {
                    sender.sendText(res.getLeft(), update);
                    return;
                }

                fsm.get(userId).setAmount(res.right().get());
                sender.sendText("Отправьте фото отчётностии", update);
                fsmStage.put(userId, 4);
            }
            case 4 -> {
                var res = photoValidator.validate(update);

                if (res.isLeft()){
                    sender.sendText(res.getLeft(), update);
                    return;
                }

                GetFile getFile = new GetFile();
                getFile.setFileId(res.right().get().getFileId());
                try {
                    File file = downloadFile(execute(getFile));

                    String id = minio.upload(file.getAbsolutePath());
                    fsm.get(userId).setPhotoId(id);

                    fsm.get(userId).setPhotoId(id);

                    Time time = fsm.get(userId);
                    time.getFios().forEach(it -> {
                        time.setFio(it);
                        clickhouse.insert(time);
                    });

                    fsm.remove(userId);
                    fsmStage.remove(userId);

                    sender.sendText("Сохранено", update);

                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private boolean processInline(Update update){
        if (!update.hasInlineQuery()) return false;

        InlineQuery inlineQuery = update.getInlineQuery();
        String text = inlineQuery.getQuery();

        AtomicInteger i = new AtomicInteger();
        List<InlineQueryResultArticle> articles = lucene.search(text)
                .stream()
                .map(it -> InlineQueryResultArticle.builder()
                        .title(it)
                        .id(String.valueOf(i.getAndIncrement()))
                        .inputMessageContent(InputTextMessageContent.builder()
                                .messageText(it)
                                .disableWebPagePreview(false)
                                .build())
                        .build())
                .toList();
        AnswerInlineQuery answerInlineQuery = AnswerInlineQuery
                .builder()
                .results(articles)
                .inlineQueryId(inlineQuery.getId())
                .isPersonal(true)
                .build();

        sender.send(answerInlineQuery, inlineQuery.getFrom());

        return true;
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
            sender.sendText("Введите фио. /complete - для окончания ввода", update);
            InlineKeyboardBuilder builder = InlineKeyboardBuilder.create()
                    .row().switchToInline().endRow();
            builder.setText("нажмите для поиска фио");
            sender.send(builder.build(update.getMessage().getChatId()), update.getMessage().getFrom());
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
        List<Subject> subjects = Arrays.stream(messasge.replace("/register " + chatId + " ", "")
                        .split(" "))
                .map(Subject::valueOf)
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
