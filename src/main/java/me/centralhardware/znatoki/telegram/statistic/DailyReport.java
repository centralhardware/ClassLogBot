package me.centralhardware.znatoki.telegram.statistic;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.mapper.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;
import java.util.Objects;

import static java.util.function.Predicate.not;

@Component
@RequiredArgsConstructor
public class DailyReport {

    private final TimeMapper timeMapper;
    private final TelegramSender sender;

    @Scheduled(cron = "* * 22 * * *")
    public void report(){
        timeMapper.getIds()
                .stream()
                .map(timeMapper::getTodayTimes)
                .filter(Objects::nonNull)
                .filter(not(List::isEmpty))
                .forEach(it -> {
                    var user = getUser(it.stream().findFirst().get());

                    sender.sendText("Занятия проведенные за сегодня",user);

                    it.forEach(time -> {
                        sender.sendText(String.format("""
                                            Время: %s,
                                            Предмет: %s
                                            Ученики: %s
                                            Стоимость: %s
                                            """,
                                time.getDateTime().getHour() + ":" + time.getDateTime().getMinute(),
                                Subject.valueOf(time.getSubject()).getRusName(),
                                String.join(", ", time.getFios()),
                                time.getAmount()),
                                user);
                    });

                });
    }

    public User getUser(Time time){
        var user = new User();
        user.setId(time.getChatId());
        user.setLanguageCode("ru");
        return user;
    }

}
