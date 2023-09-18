package me.centralhardware.znatoki.telegram.statistic.telegram;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.web.Edit;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CallbackHandler {

    private final TelegramService telegramService;
    private final TelegramUtil telegramUtil;
    private final PupilService pupilService;
    private final TelegramSender sender;

    private final TimeMapper timeMapper;


    public static final String USER_INFO_COMMAND        = "/user_info";
    public static final String DELETE_USER_COMMAND      = "/Љdelete_user";

    public boolean processCallback(Update update){
        if (!update.hasCallbackQuery()) return false;

        var callbackQuery = update.getCallbackQuery();
        var text = callbackQuery.getData();
        var from = callbackQuery.getFrom();

        Long chatId = callbackQuery.getMessage().getChatId();
        if (text.startsWith(USER_INFO_COMMAND)){
            if (!telegramService.hasReadRight(chatId)){
                sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, from);
                return true;
            }
            var pupilOptional = pupilService.findById(Integer.parseInt(callbackQuery.getData().replace(USER_INFO_COMMAND,"")));
            pupilOptional.ifPresentOrElse(
                    pupil -> sender.sendMessageWithMarkdown(pupil.toString(), callbackQuery.getFrom()),
                    () -> sender.sendMessageFromResource(MessageConstant.USER_NOT_FOUND, callbackQuery.getFrom())
            );
        } else if (text.startsWith(DELETE_USER_COMMAND)){
            if (!telegramService.isAdmin(chatId)) {
                sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, from);
                return true;
            }
            pupilService.findById(Integer.parseInt(text.replace(DELETE_USER_COMMAND,""))).ifPresent(pupil -> {
                pupil.setDeleted(true);
                pupilService.save(pupil);
                sender.sendMessageFromResource(MessageConstant.PUPIL_DELETED, from);
            });
        } else if (text.startsWith("timeDelete-")){
            if (!telegramService.isAdmin(chatId)){
                sender.sendText("Доступ запрещен", callbackQuery.getFrom());
            }

            var id = UUID.fromString(text.replace("timeDelete-", ""));
            timeMapper.setDeleted(id, true);

            var keyboard = InlineKeyboardBuilder.create()
                    .setText("?")
                    .row()
                    .button("восстановить", "timeRestore-" + id)
                    .endRow().build(callbackQuery);
            sender.send(keyboard, from);
        } else if (text.startsWith("timeRestore-")){
            if (!telegramService.isAdmin(chatId)){
                sender.sendText("Доступ запрещен", callbackQuery.getFrom());
            }

            var id = UUID.fromString(text.replace("timeRestore-", ""));
            timeMapper.setDeleted(id, false);

            var keyboard = InlineKeyboardBuilder.create()
                    .setText("?")
                    .row()
                    .button("удалить", "timeDelete-" + id)
                    .endRow().build(callbackQuery);
            sender.send(keyboard, from);
        }
        return true;
    }

}
