package me.centralhardware.znatoki.telegram.statistic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final ClientService clientService;
    private final TelegramService telegramService;
    private final TelegramSender sender;

    @Scheduled(cron = "0 0 7 * * *")
    public void notifyBirthDay() {
//        log.info("start checking birthday");
//        clientService.getAll()
//                .stream()
//                .peek(it -> log.info("check {}, date of birth = {}", it.getId(), it.getDateOfBirth()))
//                .filter(it -> DateUtils.isBirthday(it.getDateOfBirth()))
//                .forEach(pupil -> {
//                    telegramService.getReadRightUser(pupil.getOrganizationId()).forEach(id -> sender.send(SendMessage.builder().
//                            chatId(id.toString()).
//                            text(String.format("День рождения у %s %s %s телефон: %s",
//                                    pupil.getSecondName(),
//                                    pupil.getName(),
//                                    pupil.getLastName(),
//                                    pupil.getTelephone())).build(), getUser(id)));
//                });
//        log.info("finish checking birthday");
    }

    private User getUser(Long id){
        User user = new User();
        user.setId(id);
        user.setLanguageCode("ru");
        return user;
    }

}
