package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.studentCommand;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class AddPupilCommand extends CommandHandler {

    private final Storage storage;
    private final ResourceBundle resourceBundle;
    private final TelegramService telegramService;

    @Override
    public void handle(Message message) {
        if (storage.contain(message.getChatId())){
            sender.sendText("Сначала сохраните текущую запись", message.getFrom(), false);
            return;
        }

        storage.createPupil(message.getChatId());
        sender.sendMessageAndRemoveKeyboard(resourceBundle.getString("INPUT_FIO_IN_FORMAT"), message.getFrom());
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/addPupil");
    }

    @Override
    public Role getRequiredRole() {
        return Role.READ_WRITE;
    }
}
