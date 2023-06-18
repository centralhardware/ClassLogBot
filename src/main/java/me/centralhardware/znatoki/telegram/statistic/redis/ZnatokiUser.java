package me.centralhardware.znatoki.telegram.statistic.redis;

import lombok.Builder;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.Subjects;

import java.util.List;

@Builder
public record ZnatokiUser(
        List<Subjects> subjects
) { }
