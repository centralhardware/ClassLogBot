package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler;

import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;

public abstract class CallbackHandler {

    @Autowired
    protected TelegramSender sender;
    @Autowired
    protected TelegramService telegramService;
    @Autowired
    private Redis redis;

    public void handle(CallbackQuery callbackQuery){
        handle(callbackQuery, callbackQuery.getFrom(), callbackQuery.getData());
    }

    public abstract void handle(CallbackQuery callbackQuery, User from, String data);


    public abstract boolean isAcceptable(String data);

    /**
     * @return True, if giving callbackQuery can be processed by this handler
     */
    public boolean isAcceptable(CallbackQuery callbackQuery){
        String text = callbackQuery.getData();
        if (StringUtils.isBlank(text)) return false;

        return isAcceptable(callbackQuery.getData());
    }

    protected ZnatokiUser getZnatokiUser(User from){
        return redis.getUser(from.getId()).get();
    }


}
