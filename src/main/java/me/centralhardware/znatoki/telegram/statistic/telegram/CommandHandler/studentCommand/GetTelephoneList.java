package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.studentCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * get telephone list
 * param: no
 * output format: "telephone - fio"
 * input format: no
 * access level: read
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetTelephoneList extends CommandHandler {

    private final ClientService clientService;
    private final TelegramUtil telegramUtils;
    private final Redis redis;

    @Override
    public void handle(Message message) {
        if (!telegramUtils.checkReadAccess(message.getFrom(), sender)) return;

        var orgId = redis.getUser(message.getFrom().getId()).get().organizationId();
        if (clientService.getTelephone(orgId).isEmpty()){
            sender.sendMessageFromResource(MessageConstant.DATABASE_NOT_CONTAIN_TEL, message.getFrom());
            return;
        }
        clientService.getTelephone(orgId).forEach((telephone, fio) -> {
            if (StringUtils.isBlank(telephone)) return;

            sender.sendText(telephone + " " + fio, message.getFrom());
        });
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/show_telephone_list");
    }
}
