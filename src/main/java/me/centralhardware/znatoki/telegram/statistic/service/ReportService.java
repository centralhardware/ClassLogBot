package me.centralhardware.znatoki.telegram.statistic.service;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.EmployNameMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServiceMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ReportService {

    private final ServiceMapper serviceMapper;
    private final EmployNameMapper employNameMapper;
    private final Redis redis;
    private final ClientService clientService;
    private final ServicesMapper servicesMapper;

    public List<File> getReportsCurrent(Long id){
        return getReport(serviceMapper::getCuurentMontTimes, id);
    }

    public List<File> getReportPrevious(Long id){
        return getReport(serviceMapper::getPrevMonthTimes, id);
    }

    private List<File> getReport(Function<Long,List<Time>> getTime, Long id){
        var times = getTime.apply(id);
        if (CollectionUtils.isEmpty(times)) return Collections.emptyList();

        return redis.getUser(times.get(0).getChatId())
                .fold(error -> Collections.emptyList(),
                        user -> user.services()
                                .stream()
                                .map(it -> {
                                    var date = times.stream()
                                            .filter(time -> time.getServiceId().equals(it))
                                            .findFirst()
                                            .map(Time::getDateTime)
                                            .orElse(null);
                                    if (date == null) return null;

                                    return new MonthReport(employNameMapper.getFio(id), clientService, it,servicesMapper.getKeyById(it), date).generate(times);
                                })
                                .filter(Objects::nonNull)
                                .toList());
    }

}
