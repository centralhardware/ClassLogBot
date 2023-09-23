package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.Role;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps.AddOrganization;
import me.centralhardware.znatoki.telegram.statistic.utils.Transcriptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrganizationFsm extends Fsm {

    private final TelegramUtil telegramUtil;
    private final Redis redis;
    private final Transcriptor transcriptor;

    private final OrganizationMapper organizationMapper;
    private final ServicesMapper servicesMapper;

    @Override
    public void process(Update update) {
        Long userId = telegramUtil.getUserId(update);
        String text = Optional.of(update)
                .map(Update::getMessage)
                .map(Message::getText)
                .orElse(null);

        User user = telegramUtil.getFrom(update);
        switch (storage.getOrganizationStage(userId)){
            case ADD_NAME -> {
                if (StringUtils.isBlank(text)){
                    sender.sendText("Введите имя организации", user);
                    return;
                }

                storage.getOrganization(userId).setName(text);
                storage.setOrganizationStage(userId, AddOrganization.ADD_SERVICES);
                sender.sendText("Введите название оказываемых услуг. /complete для завершения.", user);
            }
            case ADD_SERVICES -> {
                if (StringUtils.isBlank(text)){
                    sender.sendText("Введите имя организации", user);
                    return;
                }

                if (!Objects.equals(text, "/complete")){
                    storage.getOrganization(userId).getServices().add(text);
                    sender.sendText("Услуга сохранена", user);
                    return;
                }


                var orgId = UUID.randomUUID();
                var znatokiUser = ZnatokiUser.builder()
                        .organizationId(orgId)
                        .role(Role.ADMIN)
                        .build();

                var org = storage.getOrganization(userId);
                org.setId(orgId);
                org.setOwner(userId);
                organizationMapper.insert(storage.getOrganization(userId));

                servicesMapper.insert(org.getServices(), orgId, transcriptor::convert);

                redis.put(user.toString(), znatokiUser);

                sender.sendText("""
                        Организация создана. 
                        Добавьте клиентов через /addPupil.
                        Заносите услуги и добавляйте оплату /addTime /addPayment
                        """, user);
            }
        }
    }

    @Override
    public boolean isActive(Long chatId) {
        return storage.containsOrganization(chatId);
    }
}
