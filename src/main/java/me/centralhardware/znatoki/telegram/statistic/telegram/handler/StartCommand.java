package me.centralhardware.znatoki.telegram.statistic.telegram.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
public class StartCommand extends CommandHandler{
    @Override
    public void handle(Message message) {
        var builder = SendMessage.builder()
                .chatId(message.getChatId())
                .text("""
                        Бот для контроля учебных часов в Знатоках.
                        Автор: @centralhardware
                        """);

        sender.send(builder.build(), message.getFrom());

    }

    @Override
    boolean isAcceptable(String data) {
        return data.equals("/start");
    }
}
