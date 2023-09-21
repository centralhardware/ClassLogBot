package me.centralhardware.znatoki.telegram.statistic.service;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.Role;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.ZnatokiUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final Redis redis;
    private final TimeMapper timeMapper;

    public List<Long> getReadRightUser(UUID orgId) {
        return timeMapper.getIds(orgId).stream()
                .filter(this::hasReadRight)
                .collect(Collectors.toList());
    }

    public boolean hasWriteRight(Long chatId) {
        return redis.getUser(chatId).get().role() == Role.READ_WRITE ||
                redis.getUser(chatId).get().role() == Role.ADMIN;
    }

    public boolean hasReadRight(Long chatId) {
        return redis.getUser(chatId).get().role() == Role.READ ||
                redis.getUser(chatId).get().role() == Role.READ_WRITE ||
                redis.getUser(chatId.toString()).get().role() == Role.ADMIN;
    }

    public boolean isAdmin(Long chatId) {
        return redis.getUser(chatId).get().role() == Role.ADMIN;
    }

    public boolean isUnauthorized(Long chatId) {
        return redis.getUser(chatId).get().role() == Role.UNAUTHORIZED;
    }

}