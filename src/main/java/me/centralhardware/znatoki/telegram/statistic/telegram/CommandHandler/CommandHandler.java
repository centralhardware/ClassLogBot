package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler;

import me.centralhardware.znatoki.telegram.statistic.telegram.Handler;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public abstract class CommandHandler implements Handler {

    @Autowired
    protected TelegramSender sender;

    public abstract void handle(Message message);

    public void handle(Update update){
        handle(update.getMessage());
    }

    public abstract boolean isAcceptable(String data);

    /**
     * @return True, if giving callbackQuery can be processed by this handler
     */
    public boolean isAcceptable(Update update){
        if (!update.hasMessage()) return false;

        String text = update.getMessage().getText();
        if (StringUtils.isBlank(text)) return false;

        return isAcceptable(update.getMessage().getText());
    }


}
