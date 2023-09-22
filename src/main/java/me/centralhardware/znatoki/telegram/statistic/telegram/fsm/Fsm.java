package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import me.centralhardware.znatoki.telegram.statistic.Config;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

public interface Fsm {

    void process(Update update);
    boolean isActive(Long chatId);

    default User getLogUser(){
        var logUser = new User();
        logUser.setId(Config.getLogChatId());
        logUser.setUserName("logger");
        logUser.setLanguageCode("ru");
        return logUser;
    }

}
