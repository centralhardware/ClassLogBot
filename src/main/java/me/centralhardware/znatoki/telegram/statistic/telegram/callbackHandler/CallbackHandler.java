package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler;

import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.Handler;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

public abstract class CallbackHandler implements Handler {

    @Autowired
    protected TelegramSender sender;
    @Autowired
    protected TelegramService telegramService;
    @Autowired
    private Redis redis;

    public void handle(CallbackQuery callbackQuery){
        handle(callbackQuery, callbackQuery.getFrom(), callbackQuery.getData());
    }

    public void handle(Update update){
        handle(update.getCallbackQuery());
    }

    public abstract void handle(CallbackQuery callbackQuery, User from, String data);


    public abstract boolean isAcceptable(String data);

    /**
     * @return True, if giving callbackQuery can be processed by this handler
     */
    public boolean isAcceptable(Update update){
        if (!update.hasCallbackQuery()) return false;

        String text = update.getCallbackQuery().getData();
        if (StringUtils.isBlank(text)) return false;

        return isAcceptable(update.getCallbackQuery().getData());
    }

    protected ZnatokiUser getZnatokiUser(User from){
        return redis.getUser(from.getId()).get();
    }


}
