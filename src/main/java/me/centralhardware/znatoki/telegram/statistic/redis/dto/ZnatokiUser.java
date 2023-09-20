package me.centralhardware.znatoki.telegram.statistic.redis.dto;

import lombok.Builder;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;

import java.util.List;
import java.util.UUID;

@Builder
public record ZnatokiUser(
        List<Subject> subjects,
        Role role,
        UUID organizationId
) { }
