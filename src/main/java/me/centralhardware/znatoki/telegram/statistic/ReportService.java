package me.centralhardware.znatoki.telegram.statistic;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.mapper.PupilMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.TeacherNameMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ReportService {

    private final TimeMapper timeMapper;
    private final TeacherNameMapper teacherNameMapper;
    private final PupilMapper pupilMapper;
    private final Redis redis;

    public List<File> getReportsCurrent(Long id){
        return getReport(timeMapper::getCuurentMontTimes, id);
    }

    public List<File> getReportPrevious(Long id){
        return getReport(timeMapper::getPrevMonthTimes, id);
    }

    private List<File> getReport(Function<Long,List<Time>> getTime, Long id){
        var times = getTime.apply(id);
        if (CollectionUtils.isEmpty(times)) return null;

        return redis.get(times.get(0).getChatId().toString(), ZnatokiUser.class)
                .subjects()
                .stream()
                .map(it -> {
                    var date = times.stream()
                            .filter(time -> Subject.valueOf(time.getSubject()).equals(it))
                            .findFirst()
                            .map(Time::getDateTime)
                            .orElse(null);
                    if (date == null) return null;

                    return new MonthReport(teacherNameMapper.getFio(id), pupilMapper, it, date).generate(times);
                })
                .filter(Objects::nonNull)
                .toList();
    }

}
