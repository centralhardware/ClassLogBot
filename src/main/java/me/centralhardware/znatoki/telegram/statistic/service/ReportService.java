package me.centralhardware.znatoki.telegram.statistic.service;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TeacherNameMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.ZnatokiUser;
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

    private final TimeMapper timeMapper;
    private final TeacherNameMapper teacherNameMapper;
    private final Redis redis;
    private final PupilService pupilService;

    public List<File> getReportsCurrent(Long id){
        return getReport(timeMapper::getCuurentMontTimes, id);
    }

    public List<File> getReportPrevious(Long id){
        return getReport(timeMapper::getPrevMonthTimes, id);
    }

    private List<File> getReport(Function<Long,List<Time>> getTime, Long id){
        var times = getTime.apply(id);
        if (CollectionUtils.isEmpty(times)) return Collections.emptyList();

        return redis.getUser(times.get(0).getChatId())
                .get()
                .subjects()
                .stream()
                .map(it -> {
                    var date = times.stream()
                            .filter(time -> Subject.valueOf(time.getSubject()).equals(it))
                            .findFirst()
                            .map(Time::getDateTime)
                            .orElse(null);
                    if (date == null) return null;

                    return new MonthReport(teacherNameMapper.getFio(id), pupilService, it, date).generate(times);
                })
                .filter(Objects::nonNull)
                .toList();
    }

}
