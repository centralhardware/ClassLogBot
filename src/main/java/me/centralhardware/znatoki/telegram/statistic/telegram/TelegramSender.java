package me.centralhardware.znatoki.telegram.statistic.telegram;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.i18n.ConstantEnum;
import me.centralhardware.znatoki.telegram.statistic.limiter.Limiter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import java.io.File;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramSender {


    private final Limiter limiter;
    private final ResourceBundle resourceBundle;
    private final ReplyKeyboardRemove replyKeyboardRemove;
    private final OkHttpTelegramClient telegramClient;
    private static final String PARSE_MODE_MARKDOWN = "markdown";

    public void sendText(String text, User user){
        sendText(text, user, true);
    }

    public void sendText(String text, Long chatId){
        var user = new User();
        user.setId(chatId);
        user.setLanguageCode("ru");
        sendText(text, user, true);
    }

    public void sendText(String text, User user, Boolean removeKeyboard){
        SendMessage.SendMessageBuilder message = SendMessage.builder()
                .chatId(user.getId())
                .text(text);

        if (removeKeyboard){
            ReplyKeyboardRemove removeMarkup = new ReplyKeyboardRemove();
            removeMarkup.setRemoveKeyboard(true);
            message.replyMarkup(removeMarkup);
        }

        send(message.build());
    }

    public Optional<File> downloadFile(GetFile getFile){
        return Try.of(() -> telegramClient.downloadFile(telegramClient.execute(getFile))).toJavaOptional();
    }

    public void send(Object method){
        limiter.limit(() -> {
            try {
                if (method instanceof BotApiMethodMessage botApiMethodMessage){
                    telegramClient.execute(botApiMethodMessage);
                } else if (method instanceof  SendPhoto sendPhoto){
                    telegramClient.execute(sendPhoto);
                } else if (method instanceof DeleteMessage deleteMessage){
                    telegramClient.execute(deleteMessage);
                } else if (method instanceof AnswerCallbackQuery answerCallbackQuery){
                    telegramClient.execute(answerCallbackQuery);
                } else if (method instanceof SendChatAction sendChatAction){
                    telegramClient.execute(sendChatAction);
                } else if (method instanceof AnswerInlineQuery answerInlineQuery){
                    telegramClient.execute(answerInlineQuery);
                } else if (method instanceof SendDocument sendDocument){
                    telegramClient.execute(sendDocument);
                } else if (method instanceof EditMessageText editMessageText){
                    telegramClient.execute(editMessageText);
                } else if (method instanceof EditMessageReplyMarkup editMessageReplyMarkup){
                    telegramClient.execute(editMessageReplyMarkup);
                }

            } catch (Throwable t){
                log.warn("",t);
            }
        });
    }

    public void sendMessageFromResource(ConstantEnum key, User from, Boolean deleteKeyboard){
        sendText(resourceBundle.getString(key.getKey()), from, deleteKeyboard);
    }

    public void sendMessageFromResource(ConstantEnum key, User from){
        sendMessageFromResource(key, from, true);
    }

    public void sendMessageAndRemoveKeyboard(String text, User user) {
        send(SendMessage.
                builder().
                chatId(user.getId()).
                text(text).
                replyMarkup(replyKeyboardRemove).
                build());
    }

    public void sendMessageWithMarkdown(String text, User user) {
        send(SendMessage.
                builder().
                chatId(user.getId()).
                text(text).
                parseMode(PARSE_MODE_MARKDOWN).
                build());
    }

}
