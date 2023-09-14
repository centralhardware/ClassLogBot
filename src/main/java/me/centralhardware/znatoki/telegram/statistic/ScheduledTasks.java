package me.centralhardware.znatoki.telegram.statistic;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.utils.DateUtils;
import me.centralhardware.znatoki.telegram.statistic.utils.TelegramUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);
    private final PupilService pupilService;
    private final TelegramService telegramService;
    private final TelegramUtils telegramUtils;
    private final TelegramSender sender;

    @Scheduled(cron = "0 0 0 31 5 *")
    public void updateClassNumber() {
        log.info("start increment classNumber");
        pupilService.getAll().forEach(pupil -> {
            if (pupil.getClassNumber() != 11 && pupil.getClassNumber() != -1) {
                pupil.incrementClassNumber();
                pupilService.save(pupil);
            }
        });
        log.info("finish increment classNumber");
    }

    @Scheduled(cron = "0 0 7 * * *")
    public void notifyBirthDay() {
        log.info("start checking birthday");
        pupilService.getAll().forEach(pupil -> {
            log.info("check {}, date of birth = {}", pupil.getId(), pupil.getDateOfBirth());
            if (DateUtils.isBirthday(pupil.getDateOfBirth())) {
                log.info("birthday user today");
                telegramService.getReadRightUser().forEach(telegramUser -> sender.send(SendMessage.builder().
                        chatId(telegramUser.getId().toString()).
                        text(String.format("День рождения у %s %s %s телефон: %s",
                                pupil.getSecondName(),
                                pupil.getName(),
                                pupil.getLastName(),
                                pupil.getTelephone())).build(), getUser(telegramUser)));
            }
        });
        log.info("finish checking birthday");
    }

    private User getUser(TelegramUser telegramUser){
        User user = new User();
        user.setId(telegramUser.getId());
        user.setUserName(telegramUser.getUsername());
        user.setFirstName(telegramUser.getFirstName());
        user.setLastName(telegramUser.getLastName());
        return user;
    }

}
