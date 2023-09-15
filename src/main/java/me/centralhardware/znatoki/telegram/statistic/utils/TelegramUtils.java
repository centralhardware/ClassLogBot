package me.centralhardware.znatoki.telegram.statistic.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.Role;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUtils {


    private static final String BOLD_MAKER = "*";

    private final TelegramSender sender;
    private final Redis redis;

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
        var znatokiUser = redis.get(user.getId().toString(), ZnatokiUser.class)
                .getOrElseThrow(() -> new IllegalStateException());

        boolean authorized = false;
        if (right.equals("read")) {
            authorized = znatokiUser.role() == Role.ADMIN ||
                    znatokiUser.role() == Role.READ ||
                    znatokiUser.role() == Role.READ_WRITE;
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
        return authorized;
    }

    public boolean checkReadAccess(User user, String operation) {
        return checkAccess(user, "read", operation);
    }

}
