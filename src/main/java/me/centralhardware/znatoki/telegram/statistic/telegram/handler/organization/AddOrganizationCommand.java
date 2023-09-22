package me.centralhardware.znatoki.telegram.statistic.telegram.handler.organization;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Storage;
import me.centralhardware.znatoki.telegram.statistic.entity.Organization;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps.AddOrganization;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class AddOrganizationCommand extends CommandHandler {

    private final Storage storage;
    private final OrganizationMapper organizationMapper;

    @Override
    public void handle(Message message) {
        if (organizationMapper.getByOwner(message.getFrom().getId()) != null){
            sender.sendText("Организация уже создана", message.getFrom());
            return;
        }

        storage.setOrganization(message.getChatId(), new Organization());
        storage.setOrganizationStage(message.getChatId(), AddOrganization.ADD_NAME);

        sender.sendText("Введите название организации", message.getFrom());
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/createOrg");
    }
}
