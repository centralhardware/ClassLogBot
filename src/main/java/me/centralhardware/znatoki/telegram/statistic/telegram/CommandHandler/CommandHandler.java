package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler;

import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.Handler;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public abstract class CommandHandler implements Handler {

    @Autowired
    protected TelegramSender sender;
    @Autowired
    private TelegramUtil telegramUtil;
    @Autowired
    private UserMapper userMapper;

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

        return isAcceptable(update.getMessage().getText()) && checkAuth(update);
    }

    public abstract Role getRequiredRole();

    public boolean checkAuth(Update update){
        var id = telegramUtil.getUserId(update);
        var user = userMapper.getById(id);

        var requiredRole = getRequiredRole();
        if (requiredRole == null) return true;

        if (user == null || user.getRole() == Role.BLOCK){
            sendAccessDenied(update);
            return false;
        }


        switch (requiredRole){
            case ADMIN -> {
                if (user.getRole() != Role.ADMIN){
                    sendAccessDenied(update);
                    return false;
                }
            }
            case READ_WRITE -> {
                if (user.getRole() != Role.ADMIN && user.getRole() != Role.READ_WRITE){
                    sendAccessDenied(update);
                    return false;
                }
            }
        }

        return true;
    }

    private void sendAccessDenied(Update update){
        sender.sendText("Недостаточно прав", TelegramUtil.getFrom(update));
    }


}
