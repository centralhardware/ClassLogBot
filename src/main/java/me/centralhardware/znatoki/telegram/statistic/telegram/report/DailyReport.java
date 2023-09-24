package me.centralhardware.znatoki.telegram.statistic.telegram.report;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.entity.Organization;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.function.Predicate.not;

@Component
@RequiredArgsConstructor
public class DailyReport {

    private final TimeMapper timeMapper;
    private final TelegramSender sender;
    private final OrganizationMapper organizationMapper;
    private final ServicesMapper servicesMapper;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Scheduled(cron = "0 0 22 * * *")
    public void report(){
        organizationMapper
                .getOwners()
                .stream()
                .map(Organization::getId)
                .map(timeMapper::getIds)
                .flatMap(Collection::stream)
                .map(timeMapper::getTodayTimes)
                .filter(Objects::nonNull)
                .filter(not(List::isEmpty))
                .forEach(it -> {
                    var user = getUser(it.stream().findFirst().get());

                    sender.sendText("Занятия проведенные за сегодня",user);

                    it.forEach(time -> sender.sendText(STR."""
                                        Время: \{timeFormatter.format(time.getDateTime())}
                                        Предмет: \{ servicesMapper.getNameById(time.getServiceId())}
                                        Ученики: \{String.join(", ", time.getFios().stream().map(Pair::getLeft).toList())}
                                        Стоимость: \{time.getAmount()}
                            """, user));
                    sender.sendText("Проверьте правильность внесенных данных",user);
                });
    }

    public User getUser(Time time){
        var user = new User();
        user.setId(time.getChatId());
        user.setLanguageCode("ru");
        return user;
    }

}
