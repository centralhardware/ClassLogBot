package me.centralhardware.znatoki.telegram.statistic.telegram;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.limiter.Limiter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramSender {


    private final TelegramUtil telegramUtil;
    private final Limiter limiter;
    @Setter
    private AbsSender absSender;

    private static final ReplyKeyboardMarkup removeMarkup = ReplyKeyboardMarkup.builder()
            .clearKeyboard()
            .build();

    public void sendText(String text, Update update){
        SendMessage message = SendMessage.builder()
                .chatId(telegramUtil.getFrom(update).getId())
                .text(text)
                .replyMarkup(removeMarkup)
                .build();
        send(message, telegramUtil.getFrom(update));
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
                }
                telegramUtil.saveStatisticOutcome(method, user);

            } catch (Throwable t){
                log.warn("",t);
            }
        });
    }
}
