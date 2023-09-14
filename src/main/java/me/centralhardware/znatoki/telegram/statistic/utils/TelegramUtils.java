package me.centralhardware.znatoki.telegram.statistic.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.entity.Enum.Role;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUtils {


    private static final String BOLD_MAKER = "*";

    private final TelegramService telegramService;
    private final TelegramSender sender;

    /**
     * @param text message to make bold
     * @return text wrapped in markdown bold symbol
     */
    public static String makeBold(String text) {
        return BOLD_MAKER + text + BOLD_MAKER + "\n";
    }

    /**
     * @param text number to make bold
     * @return text wrapped in markdown bold symbol
     */
    public static String makeBold(Integer text) {
        if (text == null) return "";

        return BOLD_MAKER + text + BOLD_MAKER;
    }

    public static final String UNAUTHORIZED_ACCESS_USER_TRY_TO_EXECUTE = "unauthorized access - user %s %s %s %s try to execute %s ";

    private boolean checkAccess(User user, String right, String operation) {
        boolean authorized = false;
        var telegramUserOptional = telegramService.findById(user.getId());
        if (telegramUserOptional.isPresent()) {
            var telegramUser = telegramUserOptional.get();
            switch (right) {
                case "read" -> {
                    if (telegramUser.hasReadRight()) authorized = true;
                }
                case "admin" -> {
                    if (telegramUser.getRole() == Role.ADMIN) authorized = true;
                }
                case "write" -> {
                    if (telegramUser.hasWriteRight()) authorized = true;
                }
            }
            if (!authorized) {
                sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, user);
                log.warn(String.format(UNAUTHORIZED_ACCESS_USER_TRY_TO_EXECUTE,
                        user.getUserName(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getId(),
                        operation));
            }
        }
        return authorized;
    }

    public boolean checkReadAccess(User user, String operation) {
        return checkAccess(user, "read", operation);
    }

    public boolean checkAdminAccess(User user, String operation) {
        return checkAccess(user, "admin", operation);
    }


}
