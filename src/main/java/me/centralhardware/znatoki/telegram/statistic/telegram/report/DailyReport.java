package me.centralhardware.znatoki.telegram.statistic.telegram.report;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Service;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Organization;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServiceMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
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

    private final ServiceMapper serviceMapper;
    private final TelegramSender sender;
    private final OrganizationMapper organizationMapper;
    private final ServicesMapper servicesMapper;
    private final ClientService clientService;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Scheduled(cron = "0 0 22 * * *")
    public void report(){
        organizationMapper
                .getOwners()
                .stream()
                .map(Organization::getId)
                .map(serviceMapper::getIds)
                .flatMap(Collection::stream)
                .map(serviceMapper::getTodayTimes)
                .filter(Objects::nonNull)
                .filter(not(List::isEmpty))
                .forEach(it -> {
                    var user = getUser(it.stream().findFirst().get());

                    sender.sendText("Занятия проведенные за сегодня",user);

                    it.forEach(service -> sender.sendText(STR."""
                                        Время: \{timeFormatter.format(service.getDateTime())}
                                        Предмет: \{ servicesMapper.getNameById(service.getServiceId())}
                                        \{organizationMapper.getById(service.getOrganizationId()).getClientName()}: \{String.join(", ", service.getServiceIds().stream().map(clientService::getFioById).toList())}
                                        Стоимость: \{service.getAmount()}
                            """, user));
                    sender.sendText("Проверьте правильность внесенных данных",user);
                });
    }

    public User getUser(Service service){
        var user = new User();
        user.setId(service.getChatId());
        user.setLanguageCode("ru");
        return user;
    }

}
