package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import me.centralhardware.znatoki.telegram.statistic.entity.Organization;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Optional;

public abstract class Fsm {

    @Autowired
    protected Storage storage;
    @Autowired
    protected TelegramSender sender;
    @Autowired
    private OrganizationMapper organizationMapper;
    @Autowired
    private Redis redis;

    abstract void process(Update update);
    abstract boolean isActive(Long chatId);

    protected Optional<User> getLogUser(Long userId){
        return redis.getUser(userId)
                .map(ZnatokiUser::organizationId)
                .map(it -> organizationMapper.getById(it))
                .map(Organization::getLogChatId)
                .map(logChatId -> {
                    var logUser = new User();
                    logUser.setId(logChatId);
                    logUser.setLanguageCode("ru");
                    return logUser;
                })
                .toJavaOptional();
    }

}
