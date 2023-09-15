package me.centralhardware.znatoki.telegram.statistic.telegram.handler.studentCommand;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Storage;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
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
        if (telegramService.isUnauthorized(message.getChatId()) || !telegramService.hasWriteRight(message.getChatId())){
            sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, message.getFrom());
            return;
        }

        storage.createPupil(message.getChatId());
        sender.sendMessageAndRemoveKeyboard(resourceBundle.getString("INPUT_FIO_IN_FORMAT"), message.getFrom());
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/addPupil");
    }
}
