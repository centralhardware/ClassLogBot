package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.organization;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AddLogChatCommand extends CommandHandler {

    private final OrganizationMapper organizationMapper;

    @Override
    public void handle(Message message) {
        if (message.getChatId() > 0){
            sender.sendText("Сначала добавте бота в чат, который хотите использовать для лога, затем выполните команду /join @@OrgStatisticBot ", message.getChatId());
            return;
        }

        var organization = organizationMapper.getByOwner(message.getFrom().getId());

        if (Objects.equals(organization.getLogChatId(), message.getChatId())){
            sender.sendText("Данный чат уже привязан к боту", message.getChatId());
        } else if  (organization.getLogChatId() != null){
            sender.sendText("Чат для логирования уже добавлен", message.getChatId());
        } else {
            organizationMapper.updateLogChat(organization.getId(), message.getChatId());
            sender.sendText("Чат сохранен. Теперь сюда будут приходить уведомления об действиях с ботом от всех сотрудников",message.getChatId());
        }
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/join");
    }

    @Override
    public Role getRequiredRole() {
        return Role.ADMIN;
    }

}
