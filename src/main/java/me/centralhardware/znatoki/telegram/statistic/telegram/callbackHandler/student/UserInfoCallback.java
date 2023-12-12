package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServiceMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.CallbackHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class UserInfoCallback extends CallbackHandler {

    private final ClientService clientService;

    private final ServiceMapper serviceMapper;
    private final ServicesMapper servicesMapper;

    @Override
    public void handle(CallbackQuery callbackQuery, User from, String data) {
        if (!telegramService.hasReadRight(from.getId())){
            sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, from);
            return;
        }
        var clientOptional = clientService.findById(Integer.parseInt(data.replace("/user_info","")));
        clientOptional.ifPresentOrElse(
                client -> {
                    if (!client.getOrganizationId().equals(getTelegramUser(from).getOrganizationId())){
                        sender.sendText("Доступ запрещен", from);
                        return;
                    }

                    sender.sendMessageWithMarkdown(client.getInfo(serviceMapper.getServicesForCLient(client.getId()).stream().map(servicesMapper::getNameById).toList()), callbackQuery.getFrom());
                },
                () -> sender.sendMessageFromResource(MessageConstant.USER_NOT_FOUND, from)
        );
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/user_info");
    }
}
