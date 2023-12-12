package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.studentCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Client;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServiceMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Optional;

/**
 * get user info by id
 * param: id of pupil
 * output format: "pupil toString"
 * input format: "/command pupil-id"
 * access level: read
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserInfoCommand extends CommandHandler {

    private final ClientService clientService;
    private final TelegramUtil telegramUtils;
    private final UserMapper userMapper;
    private final ServiceMapper serviceMapper;
    private final ServicesMapper servicesMapper;

    @Override
    public void handle(Message message) {
        var arguments = message.getText().replace("/i ", "");
        clientService.findById(Integer.valueOf(arguments)).ifPresentOrElse(
                client -> {
                    var orgId = userMapper.getById(message.getFrom().getId()).getOrganizationId();
                    if (!client.getOrganizationId().equals(orgId)){
                        sender.sendText("Доступ запрещен", message.getFrom());
                        return;
                    }

                    sender.sendMessageWithMarkdown(client.getInfo(serviceMapper.getServicesForCLient(client.getId()).stream().map(servicesMapper::getNameById).toList()), message.getFrom());
                },
                () -> sender.sendMessageFromResource(MessageConstant.PUPIL_NOT_FOUND, message.getFrom())
        );
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/i");
    }

    @Override
    public Role getRequiredRole() {
        return Role.READ;
    }

}
