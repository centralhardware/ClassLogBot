package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.CallbackHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class DeleteUserCallback extends CallbackHandler {

    private final ClientService clientService;

    @Override
    public void handle(CallbackQuery callbackQuery, User from, String data) {
        if (!telegramService.isAdmin(from.getId())) {
            sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, from);
            return;
        }

        clientService.findById(Integer.parseInt(data.replace("/delete_user",""))).ifPresent(pupil -> {

            if (!pupil.getOrganizationId().equals(getZnatokiUser(from).organizationId())){
                sender.sendText("Доступ запрещен", from);
                return;
            }

            pupil.setDeleted(true);
            clientService.save(pupil);
            sender.sendMessageFromResource(MessageConstant.PUPIL_DELETED, from);
        });
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/delete_user");
    }
}
