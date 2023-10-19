package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.organization;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class GrafanaCommand extends CommandHandler {

    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    @Override
    public void handle(Message message) {
        var user = userMapper.getById(message.getFrom().getId());
        if (user.getRole() != Role.ADMIN) {
            sender.sendText("Доступ запрещен", message.getFrom());
            return;
        }

        var org = organizationMapper.getById(user.getOrganizationId());
        sender.sendText(STR."""
                адрес: \{org.getGrafanaUrl()}
                пользователь: \{org.getGrafanaUsername()}
                пароль: \{org.getGrafanaPassword()}
                """, message.getFrom());
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/grafana");
    }
}
