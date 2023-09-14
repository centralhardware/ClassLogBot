package me.centralhardware.znatoki.telegram.statistic.telegram;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.utils.TelegramUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class CallbackHandler {

    private final TelegramService telegramService;
    private final TelegramUtils telegramUtils;
    private final PupilService pupilService;
    private final TelegramSender sender;


    public static final String USER_INFO_COMMAND        = "/user_info";
    public static final String DELETE_USER_COMMAND      = "/Љdelete_user";

    public boolean processCallback(Update update){
        if (!update.hasCallbackQuery()) return false;

        var callbackQuery = update.getCallbackQuery();

        Long chatId = callbackQuery.getMessage().getChatId();
        if (callbackQuery.getData().startsWith(USER_INFO_COMMAND)){
            if (!telegramService.hasReadRight(chatId)){
                sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, callbackQuery.getFrom());
                return true;
            }
            var pupilOptional = pupilService.findById(Integer.parseInt(callbackQuery.getData().replace(USER_INFO_COMMAND,"")));
            pupilOptional.ifPresentOrElse(
                    pupil -> sender.sendMessageWithMarkdown(pupil.toString(), callbackQuery.getFrom()),
                    () -> sender.sendMessageFromResource(MessageConstant.USER_NOT_FOUND, callbackQuery.getFrom())
            );
        } else if (callbackQuery.getData().startsWith(DELETE_USER_COMMAND)){
            if (!telegramService.isAdmin(chatId)) {
                sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, callbackQuery.getFrom());
                return true;
            }
            pupilService.findById(Integer.parseInt(callbackQuery.getData().replace(DELETE_USER_COMMAND,""))).ifPresent(pupil -> {
                pupil.setDeleted(true);
                pupilService.save(pupil);
                sender.sendMessageFromResource(MessageConstant.PUPIL_DELETED, callbackQuery.getFrom());
            });
        }
        return true;
    }

}
