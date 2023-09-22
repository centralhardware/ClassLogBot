package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.Storage;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

public abstract class Fsm {

    @Autowired
    protected Storage storage;
    @Autowired
    protected TelegramSender sender;

    abstract void process(Update update);
    abstract boolean isActive(Long chatId);

    protected User getLogUser(){
        var logUser = new User();
        logUser.setId(Config.getLogChatId());
        logUser.setUserName("logger");
        logUser.setLanguageCode("ru");
        return logUser;
    }

}
