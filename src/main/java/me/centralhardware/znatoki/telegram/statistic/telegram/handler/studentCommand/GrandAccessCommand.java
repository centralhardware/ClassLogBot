package me.centralhardware.znatoki.telegram.statistic.telegram.handler.studentCommand;

import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.entity.Enum.Role;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
import me.centralhardware.znatoki.telegram.statistic.utils.TelegramUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * grant access to specific uer
 * param: chat id -
 * output format: "message with result"
 * input format "/command chat-id"
 * access level: admin
 * note: giving id must be register in database
 */
@Slf4j
@Component
public class GrandAccessCommand extends CommandHandler {

    private final TelegramService telegramService;
    private final TelegramUtils telegramUtils;

    public GrandAccessCommand(TelegramService telegramService,
                              TelegramUtils telegramUtils) {
//        super("/grant_access",
//                """
//                        изменить права доступа пользователя бота. Аргументы: --id пользователя-- --r|rw|admin--. Пример:\s
//
//                        <code> /grant_access 522104700 rw </code>
//
//                        Уровни доступа:\s
//                        r - чтение
//                        rw - чтение и запись
//                        admin - возможность менять права другим пользователям
//                        """, telegramService, statisticService);
        this.telegramService    = telegramService;
        this.telegramUtils      = telegramUtils;
    }

    @Override
    public void handle(Message message) {
        if (!telegramUtils.checkAdminAccess(message.getFrom(), "/grant_access")) return;

        var arguments = message.getText().replace("/grant_access", "").split(" ");
        if (!StringUtils.isNumeric(arguments[0])) {
            sender.sendMessageFromResource(ErrorConstant.INVALID_ARGUMENT_GRANT_ACCESS, message.getFrom());
            return;
        }
        telegramService.findById(Long.parseLong(arguments[0])).ifPresentOrElse(
                telegramUser -> {
                    switch (arguments[1]) {
                        case "r"        -> telegramUser.setRole(Role.READ);
                        case "rw"       -> telegramUser.setRole(Role.READ_WRITE);
                        case "admin"    -> telegramUser.setRole(Role.ADMIN);
                        case "no"       -> telegramUser.setRole(Role.UNAUTHORIZED);
                        default         -> {
                            sender.sendMessageFromResource(ErrorConstant.INVALID_RIGHT, message.getFrom());
                            return;
                        }
                    }
                    telegramService.save(telegramUser);
                    sender.sendMessageFromResource(MessageConstant.RIGHT_SUCCESS_UPDATE, message.getFrom());
                },
                () ->  sender.sendMessageFromResource(MessageConstant.USER_NOT_FOUND, message.getFrom())
        );
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/grant_access");
    }
}