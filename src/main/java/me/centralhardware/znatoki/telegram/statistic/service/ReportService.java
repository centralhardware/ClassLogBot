package me.centralhardware.znatoki.telegram.statistic.service;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Service;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.*;
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ReportService {

    private final ServiceMapper serviceMapper;
    private final UserMapper userMapper;
    private final ClientService clientService;
    private final ServicesMapper servicesMapper;
    private final OrganizationMapper organizationMapper;

    public List<File> getReportsCurrent(Long id){
        return getReport(serviceMapper::getCuurentMontTimes, id);
    }

    public List<File> getReportPrevious(Long id){
        return getReport(serviceMapper::getPrevMonthTimes, id);
    }

    private List<File> getReport(Function<Long,List<Service>> getTime, Long id){
        var times = getTime.apply(id);
        if (CollectionUtils.isEmpty(times)) return Collections.emptyList();

        var user =  Optional.ofNullable(userMapper.getById(times.get(0).getChatId()));
        return user.map(telegramUser -> telegramUser.getServices()
                .stream()
                .map(it -> {
                    var date = times.stream()
                            .filter(time -> time.getServiceId().equals(it))
                            .findFirst()
                            .map(Service::getDateTime)
                            .orElse(null);
                    if (date == null) return null;

                    return new MonthReport(userMapper.getById(id).getName(),
                            it, servicesMapper.getKeyById(it),
                            date,
                            organizationMapper.getReportFields(telegramUser.getOrganizationId()),
                            organizationMapper.getById(telegramUser.getOrganizationId()).getClientName()).generate(times);
                })
                .filter(Objects::nonNull)
                .toList()).orElse(Collections.emptyList());
    }

}
