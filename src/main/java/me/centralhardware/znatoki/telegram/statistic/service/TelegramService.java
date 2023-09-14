package me.centralhardware.znatoki.telegram.statistic.service;

import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.entity.Enum.Role;
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser;
import me.centralhardware.znatoki.telegram.statistic.repository.TelegramUserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;
import java.util.Optional;

@Service
public class TelegramService {

    private final TelegramUserRepository repository;

    public TelegramService(TelegramUserRepository repository) {
        this.repository = repository;
    }

    public void update(User user, Long chatId) {
        if (!repository.existsById(user.getId())) {
            TelegramUser telegramUser;
            telegramUser = new TelegramUser(chatId,
                    user.getUserName(),
                    user.getFirstName(),
                    user.getLastName(),
                    Role.UNAUTHORIZED);
            if (user.getId().equals(Config.getAdminId())) {
                telegramUser = new TelegramUser(chatId,
                        user.getUserName(),
                        user.getFirstName(),
                        user.getLastName(),
                        Role.ADMIN);
            }
            repository.save(telegramUser);
        } else {
            TelegramUser telegramUser = repository.findById(user.getId()).get();
            telegramUser.setFirstName(user.getFirstName());
            telegramUser.setLastName(user.getLastName());
            telegramUser.setUsername(user.getUserName());
            if (user.getId().equals(Config.getAdminId())) {
                telegramUser.setRole(Role.ADMIN);
            }
            repository.save(telegramUser);
        }
    }

    public Optional<TelegramUser> findById(Long id) {
        return repository.findById(id);
    }

    public List<TelegramUser> getAll() {
        return repository.findAll();
    }

    public List<TelegramUser> getReadRightUser() {
        return getAll().stream().filter(TelegramUser::hasReadRight).toList();
    }

    public void save(TelegramUser telegramUser) {
        repository.save(telegramUser);
    }

    public boolean hasWriteRight(Long chatId) {
        return repository.findById(chatId).map(TelegramUser::hasWriteRight).orElse(false);
    }

    public boolean hasReadRight(Long chatId) {
        return repository.findById(chatId).map(TelegramUser::hasReadRight).orElse(false);
    }

    public boolean isAdmin(Long chatId) {
        return repository.findById(chatId).filter(telegramUser -> telegramUser.getRole() == Role.ADMIN).isPresent();
    }

    public boolean isUnauthorized(Long chatId) {
        return repository.findById(chatId).filter(telegramUser -> telegramUser.getRole() == Role.UNAUTHORIZED).isPresent();
    }

}