package me.centralhardware.znatoki.telegram.statistic.telegram.handler.studentCommand;

import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
import me.centralhardware.znatoki.telegram.statistic.utils.TelegramUtils;
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
public class GetTelephoneList extends CommandHandler {

    private final PupilService pupilService;
    private final TelegramUtils telegramUtils;

    public GetTelephoneList(PupilService pupilService,
                            TelegramUtils telegramUtils) {
//        super("/show_telephone_list",
//                """
//                        получить список телефонов.
//                        """, telegramService, statisticService);
        this.pupilService       = pupilService;
        this.telegramUtils      = telegramUtils;
    }


    @Override
    public void handle(Message message) {
        if (!telegramUtils.checkReadAccess(message.getFrom(), "/show_telephone_list")) return;

        if (pupilService.getTelephone().isEmpty()){
            sender.sendMessageFromResource(MessageConstant.DATABASE_NOT_CONTAIN_TEL, message.getFrom());
            return;
        }
        pupilService.getTelephone().forEach((telephone,fio) -> {
            if (StringUtils.isBlank(telephone)) return;

            sender.sendText(telephone + " " + fio, message.getFrom());
        });
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/show_telephone_list");
    }
}
