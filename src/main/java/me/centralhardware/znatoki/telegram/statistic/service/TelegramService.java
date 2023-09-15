package me.centralhardware.znatoki.telegram.statistic.service;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.Role;
import me.centralhardware.znatoki.telegram.statistic.redis.ZnatokiUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final Redis redis;
    private final TimeMapper timeMapper;

    public List<Long> getReadRightUser() {
        return timeMapper.getIds().stream()
                .filter(this::hasReadRight)
                .collect(Collectors.toList());
    }

    public boolean hasWriteRight(Long chatId) {
        return redis.get(chatId.toString(), ZnatokiUser.class).get().role() == Role.READ_WRITE ||
                redis.get(chatId.toString(), ZnatokiUser.class).get().role() == Role.ADMIN;
    }

    public boolean hasReadRight(Long chatId) {
        return redis.get(chatId.toString(), ZnatokiUser.class).get().role() == Role.READ;
    }

    public boolean isAdmin(Long chatId) {
        return redis.get(chatId.toString(), ZnatokiUser.class).get().role() == Role.ADMIN;
    }

    public boolean isUnauthorized(Long chatId) {
        return redis.get(chatId.toString(), ZnatokiUser.class).get().role() == Role.UNAUTHORIZED;
    }

}