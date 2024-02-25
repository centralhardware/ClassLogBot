package me.centralhardware.znatoki.telegram.statistic.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.entity.clickhouse.LogEntry;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.StatisticMapper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import java.time.LocalDateTime;
import java.util.Map;

import static java.util.Map.entry;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramUtil {

    private final StatisticMapper statisticMapper;

    public Long getUserId(Update update){
        if (update.hasMessage()){
            return update.getMessage().getFrom().getId();
        } else if (update.hasCallbackQuery()){
            return update.getCallbackQuery().getFrom().getId();
        } else if (update.hasInlineQuery()){
            return update.getInlineQuery().getFrom().getId();
        }

        throw new IllegalStateException();
    }

    public static User getFrom(Update update){
        if (update.hasMessage()){
            return update.getMessage().getFrom();
        } else if (update.hasCallbackQuery()){
            return update.getCallbackQuery().getFrom();
        } else if (update.hasInlineQuery()){
            return update.getInlineQuery().getFrom();
        }

        throw new IllegalStateException();
    }

    private static String getText(Update update){
        if (update.hasMessage()){
            return update.getMessage().getText();
        } else if (update.hasCallbackQuery()){
            return update.getCallbackQuery().getData();
        } else if (update.hasInlineQuery()){
            return update.getInlineQuery().getQuery();
        }

        throw new IllegalStateException();
    }

    public void logUpdate(Update update){
        if (update.hasMessage()){
            Message message = update.getMessage();
            log.info("Receive message username: {}, firstName: {}, lastName: {}, id: {}, text: {}",
                    message.getFrom().getUserName(),
                    message.getFrom().getFirstName(),
                    message.getFrom().getLastName(),
                    message.getFrom().getId(),
                    message.getText());
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            log.info("Receive callback username: {}, firstName: {}, lastName: {}, id: {}, callbackDate: {}",
                    callbackQuery.getFrom().getUserName(),
                    callbackQuery.getFrom().getFirstName(),
                    callbackQuery.getFrom().getLastName(),
                    callbackQuery.getFrom().getId(),
                    callbackQuery.getData());
        }
    }

    public void saveStatisticIncome(Update update){
        String action;
        if (update.hasMessage()){
            action = "receiveText";
        } else if (update.hasCallbackQuery()){
            action = "receiveCallback";
        } else if (update.hasInlineQuery()){
            action = "receiveInlineQuery";
        }else {
            throw new IllegalStateException();
        }

        var entry = LogEntry.builder()
                .dateTime(LocalDateTime.now())
                .chatId(getUserId(update))
                .username("@" + getFrom(update).getUserName())
                .firstName(getFrom(update).getFirstName())
                .lastName(getFrom(update).getLastName())
                .isPremium(getFrom(update).getIsPremium() != null && getFrom(update).getIsPremium())
                .lang(getFrom(update).getLanguageCode())
                .text(getText(update) == null? "": getText(update))
                .build();

        statisticMapper.insertStatistic(entry);
        log.info(STR."Save to clickHouse income(\{entry.chatId()}, \{entry.text()})");
    }

    public void saveStatisticOutcome(Object object, User user){
        String chatId;
        String text;
        switch (object) {
            case SendMessage sendMessage -> {
                chatId = sendMessage.getChatId();
                text = sendMessage.getText();
            }
            case SendPhoto sendPhoto -> {
                chatId = sendPhoto.getChatId();
                text = sendPhoto.getCaption();
            }
            case DeleteMessage deleteMessage -> {
                chatId = deleteMessage.getChatId();
                text = deleteMessage.getMessageId().toString();
            }
            case SendChatAction sendChatAction -> {
                chatId = sendChatAction.getChatId();
                text = sendChatAction.getActionType().toString();
            }
            case AnswerCallbackQuery answerCallbackQuery -> {
                return;
            }
            case AnswerInlineQuery answerInlineQuery -> {
                return;
            }
            case SendDocument sendDocument -> {
                return;
            }
            case ReplyKeyboardRemove replyKeyboardRemove -> {
                return;
            }
            case EditMessageText editMessageText -> {
                chatId = editMessageText.getChatId();
                text = editMessageText.getText();
            }
            case EditMessageReplyMarkup editMessageReplyMarkup -> {
                return;
            }
            case null, default -> throw new IllegalStateException();
        }

        var entry = LogEntry.builder()
                .dateTime(LocalDateTime.now())
                .chatId(Long.valueOf(chatId))
                .username("@" + user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isPremium(user.getIsPremium() != null && user.getIsPremium())
                .lang(user.getLanguageCode())
                .text(text)
                .build();

        statisticMapper.insertStatistic(entry);
        log.info(STR."Save to clickHouse outcome(\{entry.chatId()}, \{entry.text()})");
    }

    public void logSend(Object send){
        if (send instanceof SendMessage sendMessage){
            log.info("Send message to id: {}, text: {}",
                    sendMessage.getChatId(),
                    sendMessage.getText());
        } else if (send instanceof SendPhoto sendPhoto){
            log.info("Send photo to id: {} with caption {}",
                    sendPhoto.getChatId(),
                    sendPhoto.getCaption());
        } else if (send instanceof DeleteMessage deleteMessage){
            log.info("Delete messageId: {} from chat: {}",
                    deleteMessage.getMessageId(),
                    deleteMessage.getChatId());
        } else if (send instanceof SendChatAction sendChatAction){
            log.info("Send chat action {} to {}",
                    sendChatAction.getAction(),
                    sendChatAction.getChatId());
        }
    }

    private static final String BOLD_MAKER = "*";


    /**
     * @param text message to make bold
     * @return text wrapped in markdown bold symbol
     */
    public static String makeBold(String text) {
        return BOLD_MAKER + text + BOLD_MAKER;
    }

    /**
     * @param text number to make bold
     * @return text wrapped in markdown bold symbol
     */
    public static String makeBold(Integer text) {
        if (text == null) return "";

        return BOLD_MAKER + text + BOLD_MAKER;
    }

}
