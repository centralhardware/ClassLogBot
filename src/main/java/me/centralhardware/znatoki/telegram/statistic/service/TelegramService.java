package me.centralhardware.znatoki.telegram.statistic.service;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.TelegramUser;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final UserMapper userMapper;

    public boolean hasWriteRight(Long chatId) {
        var role = getRole(chatId);
        return role == Role.READ_WRITE ||
                role == Role.ADMIN;
    }

    public boolean hasReadRight(Long chatId) {
        var role = getRole(chatId);
        return role == Role.READ ||
                role == Role.READ_WRITE ||
                role == Role.ADMIN;
    }

    public boolean isAdmin(Long chatId) {
        return getRole(chatId) == Role.ADMIN;
    }

    public boolean isUnauthorized(Long chatId) {
        return getRole(chatId) == null;
    }

    private Role getRole(Long chatId){
        return Optional.ofNullable(userMapper.getById(chatId))
                .map(TelegramUser::getRole)
                .orElse(null);
    }

}