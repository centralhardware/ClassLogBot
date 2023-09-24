package me.centralhardware.znatoki.telegram.statistic.telegram.handler.studentCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
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
@RequiredArgsConstructor
public class UserInfoCommand extends CommandHandler {

    private final PupilService pupilService;
    private final TelegramUtil telegramUtils;
    private final Redis redis;
    private final TimeMapper timeMapper;
    private final ServicesMapper servicesMapper;

    @Override
    public void handle(Message message) {
        if (!telegramUtils.checkReadAccess(message.getFrom(), sender)) return;

        var arguments = message.getText().replace("/i ", "");
        Optional<Pupil> pupilOptional = pupilService.findById(Integer.valueOf(arguments));
        pupilOptional.ifPresentOrElse(
                pupil -> {
                    var orgId = redis.getUser(message.getFrom().getId()).get().organizationId();
                    if (!pupil.getOrganizationId().equals(orgId)){
                        sender.sendText("Доступ запрещен", message.getFrom());
                        return;
                    }

                    sender.sendMessageWithMarkdown(pupil.getInfo(timeMapper.getServicesForPupil(pupil.getId()).stream().map(servicesMapper::getNameById).toList()), message.getFrom());
                },
                () -> sender.sendMessageFromResource(MessageConstant.PUPIL_NOT_FOUND, message.getFrom())
        );
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/i");
    }
}
