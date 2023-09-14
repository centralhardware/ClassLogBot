package me.centralhardware.znatoki.telegram.statistic.telegram.handler.studentCommand;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Storage;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
import me.centralhardware.znatoki.telegram.statistic.utils.TelegramUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class AddPupilCommand extends CommandHandler {

    private final Storage storage;
    private final TelegramUtils telegramUtils;
    private final ResourceBundle resourceBundle;

    @Override
    public void handle(Message message) {
        storage.createPupil(message.getChatId());
        sender.sendMessageAndRemoveKeyboard(resourceBundle.getString("INPUT_FIO_IN_FORMAT"), message.getFrom());
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/addPupil");
    }
}
