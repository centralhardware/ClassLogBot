package me.centralhardware.znatoki.telegram.statistic.redis.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ZnatokiUser(
        List<Long> services,
        Role role,
        UUID organizationId
) { }
