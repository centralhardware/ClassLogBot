package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class ResetCommand extends CommandHandler {

    private final Storage storage;

    @Override
    public void handle (Message message) {
        storage.remove(message.getChatId());
        sender.sendText("Состояние сброшено", message.getFrom());
    }

    @Override
    public boolean isAcceptable (String data) {
        return data.equalsIgnoreCase("/reset");
    }
}
