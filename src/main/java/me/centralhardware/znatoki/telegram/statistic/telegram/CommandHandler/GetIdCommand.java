package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * show chat id
 * param: no
 * output format: "id"
 * input format: no
 * access level: unauthorized
 */
@Slf4j
@Component
public class GetIdCommand extends CommandHandler {

    @Override
    public void handle(Message message) {
        sender.sendText(String.valueOf(message.getChatId()), message.getFrom());
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/get_id");
    }
}
