package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class StartCommand extends CommandHandler {

    private final OrganizationMapper organizationMapper;

    @Override
    public void handle(Message message) {
        sender.sendText("""
                        Бот предназначенный для анализа вашего бизнеса.
                        Автор: @centralhardware
                        """, message.getFrom());
        if (!organizationMapper.exist(message.getFrom().getId())){
            sender.sendText(Config.Telegram.getStartTelegraph(), message.getFrom());
        }
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equals("/start");
    }

    @Override
    public Role getRequiredRole() {
        return null;
    }

}
