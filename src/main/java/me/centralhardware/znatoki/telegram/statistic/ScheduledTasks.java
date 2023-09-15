package me.centralhardware.znatoki.telegram.statistic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.utils.DateUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final PupilService pupilService;
    private final TelegramService telegramService;
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
                telegramService.getReadRightUser().forEach(id -> sender.send(SendMessage.builder().
                        chatId(id.toString()).
                        text(String.format("День рождения у %s %s %s телефон: %s",
                                pupil.getSecondName(),
                                pupil.getName(),
                                pupil.getLastName(),
                                pupil.getTelephone())).build(), getUser(id)));
            }
        });
        log.info("finish checking birthday");
    }

    private User getUser(Long id){
        User user = new User();
        user.setId(id);
        return user;
    }

}
