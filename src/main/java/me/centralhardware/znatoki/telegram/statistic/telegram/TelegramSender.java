package me.centralhardware.znatoki.telegram.statistic.telegram;

import io.vavr.control.Try;
import jakarta.persistence.criteria.From;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.i18n.ConstantEnum;
import me.centralhardware.znatoki.telegram.statistic.limiter.Limiter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import java.io.File;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramSender {


    private final TelegramUtil telegramUtil;
    private final Limiter limiter;
    private final ResourceBundle resourceBundle;
    private final ReplyKeyboardRemove replyKeyboardRemove;
    @Setter
    private DefaultAbsSender absSender;

    public static final String UNABLE_TO_SEND_MESSAGE = "unable to send message";
    public static final String SEND_MESSAGE_FOR_CHAT = "send message %s for chat %s";
    private static final String PARSE_MODE_MARKDOWN = "markdown";

    public void sendText(String text, User user){
        SendMessage.SendMessageBuilder message = SendMessage.builder()
                .chatId(user.getId())
                .text(text);

        ReplyKeyboardRemove removeMarkup = new ReplyKeyboardRemove();
        removeMarkup.setRemoveKeyboard(true);
        message.replyMarkup(removeMarkup);

        send(message.build(), user);
    }

    public Try<File> downloadFile(GetFile getFile){
        return Try.of(() -> absSender.downloadFile(absSender.execute(getFile)));
    }

    public void send(Object method, User user){
        limiter.limit(() -> {
            try {
                telegramUtil.logSend(method);
                if (method instanceof BotApiMethodMessage botApiMethodMessage){
                    absSender.execute(botApiMethodMessage);
                } else if (method instanceof  SendPhoto sendPhoto){
                    absSender.execute(sendPhoto);
                } else if (method instanceof DeleteMessage deleteMessage){
                    absSender.execute(deleteMessage);
                } else if (method instanceof AnswerCallbackQuery answerCallbackQuery){
                    absSender.execute(answerCallbackQuery);
                } else if (method instanceof SendChatAction sendChatAction){
                    absSender.execute(sendChatAction);
                } else if (method instanceof AnswerInlineQuery answerInlineQuery){
                    absSender.execute(answerInlineQuery);
                } else if (method instanceof SendDocument sendDocument){
                    absSender.execute(sendDocument);
                }
                telegramUtil.saveStatisticOutcome(method, user);

            } catch (Throwable t){
                log.warn("",t);
            }
        });
    }

    public void sendMessageFromResource(ConstantEnum key, User from){
        sendText(resourceBundle.getString(key.getKey()), from);
    }

    public void sendMessageAndRemoveKeyboard(String text, User user) {
        send(SendMessage.
                builder().
                chatId(user.getId()).
                text(text).
                replyMarkup(replyKeyboardRemove).
                build(), user);
    }

    public void sendMessageWithMarkdown(String text, User user) {
        send(SendMessage.
                builder().
                chatId(user.getId()).
                text(text).
                parseMode(PARSE_MODE_MARKDOWN).
                build(), user);
    }

}
