package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.InvitationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.validate.ServiceValidator;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InvitationFsm  extends Fsm{

    private final TelegramUtil telegramUtil;
    private final OrganizationMapper organizationMapper;
    private final InvitationMapper invitationMapper;
    private final ServiceValidator serviceValidator;
    private final ServicesMapper servicesMapper;

    @Override
    public void process(Update update) {
        Long userId = telegramUtil.getUserId(update);
        String text = Optional.of(update)
                .map(Update::getMessage)
                .map(Message::getText)
                .orElse(null);

        User user = telegramUtil.getFrom(update);
        var orgId = organizationMapper.getByOwner(userId).getId();
        switch (storage.getInvitationStage(userId)){
            case ADD_SERVICES -> {
                if (Objects.equals(text, "/complete")){
                    var invitation = storage.getInvitation(userId);
                    invitation.setOrgId(orgId);
                    invitation.setConfirmCode(RandomStringUtils.random(6, true, true));
                    invitationMapper.insert(invitation);

                    sender.sendText(STR."""
                                        Сотруднику необходимо использовать следующую команюу для присоединения к организации.
                                        /join \{invitation.getConfirmCode()}
                            """, user);
                    return;
                }


                serviceValidator.validate(Pair.of(text, orgId))
                        .peekLeft(error -> sender.sendText(error, user))
                        .peek(service -> {
                            storage.getInvitation(userId).getServices().add(servicesMapper.getServiceId(orgId,service));
                            sender.sendText("Сохранено", user, false);
                        });
            }
        }
    }

    @Override
    public boolean isActive(Long chatId) {
        return storage.containsInvitation(chatId);
    }
}
