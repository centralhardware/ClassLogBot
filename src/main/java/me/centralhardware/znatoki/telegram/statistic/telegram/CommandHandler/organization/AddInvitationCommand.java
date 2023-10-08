package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.organization;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Invitation;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps.AddInvitation;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class AddInvitationCommand extends CommandHandler {

    private final OrganizationMapper organizationMapper;
    private final ServicesMapper servicesMapper;

    private final Storage storage;

    @Override
    public void handle(Message message) {
        var org = organizationMapper.getByOwner(message.getFrom().getId());
        if (org == null){
            sender.sendText("Необходимо быть владельцем организации", message.getFrom());
            return;
        }

        var builder = ReplyKeyboardBuilder.create()
                .setText("Выберите услуги, которые будет оказывать сотрудник. /complete для завершения");

        servicesMapper.getServicesByOrganization(org.getId())
                .forEach(service -> builder.row()
                        .button(service.getName())
                        .endRow());

        storage.setInvitation(message.getFrom().getId(), new Invitation());
        storage.setInvitationStage(message.getFrom().getId(), AddInvitation.INPUT_SERVICES);

        sender.send(builder.build(message.getChatId()), message.getFrom());
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/addInvitation");
    }
}
