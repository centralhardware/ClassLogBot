package me.centralhardware.znatoki.telegram.statistic.telegram.handler.studentCommand;

import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
import me.centralhardware.znatoki.telegram.statistic.utils.TelegramUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Optional;

/**
 * get user info by id
 * param: id of pupil
 * output format: "pupil toString"
 * input format: "/command pupil-id"
 * access level: read
 */
@Slf4j
@Component
public class UserInfoCommand extends CommandHandler {

    private final PupilService pupilService;
    private final TelegramUtils telegramUtils;

    public UserInfoCommand(PupilService pupilService,
                           TelegramUtils telegramUtils) {
//        super("/i",
//                """
//                        Вывести данные ученика по его ID. Пример:
//
//                        <code> /i 1</code>
//                        """, telegramService, statisticService);
        this.pupilService       = pupilService;
        this.telegramUtils      = telegramUtils;
    }

    @Override
    public void handle(Message message) {
        if (!telegramUtils.checkReadAccess(message.getFrom(), "/i")) return;

        var arguments = message.getText().replace("/i ", "");
        Optional<Pupil> pupilOptional = pupilService.findById(Integer.valueOf(arguments));
        pupilOptional.ifPresentOrElse(
                pupil -> sender.sendMessageWithMarkdown(pupil.toString(), message.getFrom()),
                () -> sender.sendMessageFromResource(MessageConstant.PUPIL_NOT_FOUND, message.getFrom())
        );
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/i");
    }
}
