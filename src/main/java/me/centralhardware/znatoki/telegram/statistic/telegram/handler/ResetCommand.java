package me.centralhardware.znatoki.telegram.statistic.telegram.handler;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Storage;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class ResetCommand extends CommandHandler {

    private final Storage storage;

    @Override
    public void handle (Message message) {
        storage.remove(message.getChatId());
    }

    @Override
    public boolean isAcceptable (String data) {
        return data.equalsIgnoreCase("/reset");
    }
}
