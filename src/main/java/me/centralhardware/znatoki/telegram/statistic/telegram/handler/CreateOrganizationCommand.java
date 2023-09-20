package me.centralhardware.znatoki.telegram.statistic.telegram.handler;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateOrganizationCommand extends CommandHandler {

    private final OrganizationMapper organizationMapper;

    @Override
    public void handle(Message message) {
        if (message.getText().equalsIgnoreCase("/createOrg")){
            sender.sendText("Необходимо ввести название организации через пробел после команды. Например: /createOrg example", message.getFrom());
            return;
        }

        if (organizationMapper.getByOwner(message.getFrom().getId()) != null){
            sender.sendText("Организация уже создана", message.getFrom());
            return;
        }

        var name = message.getText().replace("/createOrg", "").trim();
        organizationMapper.insert(UUID.randomUUID(), name, message.getFrom().getId());
        sender.sendText("Организация создана", message.getFrom());
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/createOrg");
    }
}
