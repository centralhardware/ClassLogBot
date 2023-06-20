package me.centralhardware.znatoki.telegram.statistic.telegram.handler;

import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Message;

public abstract class CommandHandler {

    @Autowired
    protected TelegramSender sender;

    public abstract void handle(Message message);


    abstract boolean isAcceptable(String data);

    /**
     * @return True, if giving callbackQuery can be processed by this handler
     */
    public boolean isAcceptable(Message message){
        String text = message.getText();
        if (StringUtils.isBlank(text)) return false;

        return isAcceptable(message.getText());
    }


}
