package me.centralhardware.znatoki.telegram.statistic.redis.dto;

import lombok.Builder;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;

import java.util.List;

@Builder
public record ZnatokiUser(
        List<Subject> subjects,
        Role role
) { }
