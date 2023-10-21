package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Client;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServiceMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class PupilFsm extends Fsm {

    private final TelegramService telegramService;
    private final ClientService clientService;
    private final TelegramSender sender;

    private final ServiceMapper serviceMapper;
    private final ServicesMapper servicesMapper;
    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;

    @Override
    public void process(Update update) {
        Long chatId = telegramUtil.getUserId(update);
        String text = update.getMessage().getText();
        var user = telegramUtil.getFrom(update);
        var telegramUser = userMapper.getById(chatId);

        if (telegramService.isUnauthorized(chatId) || !telegramService.hasWriteRight(chatId)){
            sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, user);
            return;
        }

        switch (storage.getPupilStage(chatId)){
            case ФВВ_FIO -> {
                String[] words = text.split(" ");
                if (!(words.length >= 2 && words.length <= 3)) {
                    sender.sendMessageFromResource(MessageConstant.INPUT_FIO_REQUIRED_FORMAT, user);
                    return;
                }
                Client client = storage.getPupil(chatId);
                if (words.length == 3){
                    client.setSecondName(words[0]);
                    client.setName(words[1]);
                    client.setLastName(words[2]);
                } else {
                    client.setSecondName(words[0]);
                    client.setName(words[1]);
                    client.setLastName("");
                }
                if (clientService.checkExistenceByFio(client.getName(), client.getSecondName(), client.getLastName())){
                    sender.sendMessageFromResource(MessageConstant.FIO_ALREADY_IN_DATABASE, user);
                    return;
                }

                var org = organizationMapper.getById(telegramUser.getOrganizationId());

                if (org.getClientCustomProperties() == null ||
                        org.getClientCustomProperties().isEmpty()){
                    getPupil(chatId).setOrganizationId(userMapper.getById(chatId).getOrganizationId());
                    getPupil(chatId).setCreated_by(chatId);
                    sender.sendMessageWithMarkdown(clientService.save(getPupil(chatId)).getInfo(serviceMapper.getServicesForPupil(getPupil(chatId).getId()).stream().map(servicesMapper::getNameById).toList()), user);
                    sendLog(getPupil(chatId), chatId);
                    storage.remove(chatId);
                    sender.sendMessageFromResource(MessageConstant.CREATE_PUPIL_FINISHED, user);
                } else {
                    next(chatId);
                    storage.getPupil(chatId).setPropertiesBuilder(new PropertiesBuilder(org.getClientCustomProperties().propertyDefs()));
                    var next = storage.getPupil(chatId).getPropertiesBuilder().getNext().get();
                    if (!next.getRight().isEmpty()){
                        var builder = ReplyKeyboardBuilder
                                .create()
                                .setText(next.getLeft());
                        next.getRight().forEach(it -> builder.row().button(it).endRow());
                        sender.send(builder.build(chatId), user);
                    } else {
                        sender.sendText(next.getLeft(), user);
                    }
                }
            }
            case ADD_PROPERTIES -> processCustomProperties(update, getPupil(chatId).getPropertiesBuilder(), properties -> {
                getPupil(chatId).setOrganizationId(userMapper.getById(chatId).getOrganizationId());
                getPupil(chatId).setCreated_by(chatId);
                getPupil(chatId).setProperties(properties);
                sender.sendMessageWithMarkdown(clientService.save(getPupil(chatId)).getInfo(serviceMapper.getServicesForPupil(getPupil(chatId).getId()).stream().map(servicesMapper::getNameById).toList()), user);
                sendLog(getPupil(chatId), chatId);
                storage.remove(chatId);
                sender.sendMessageFromResource(MessageConstant.CREATE_PUPIL_FINISHED, user);
            });
        }
    }

    public Client getPupil(Long chatId){
        return storage.getPupil(chatId);
    }

    public void next(Long chatId){
        var next = storage.getPupilStage(chatId).next();
        storage.setPupilStage(chatId, next);
    }

    @Override
    public boolean isActive(Long chatId) {
        return storage.containsPupil(chatId);
    }

    private void sendLog(Client client, Long chatId) {
        getLogUser(chatId)
                .ifPresent(user -> {
                    var message = SendMessage.builder()
                            .text(STR."#\{organizationMapper.getById(client.getOrganizationId()).getClientName()}\n" + client.getInfo(serviceMapper.getServicesForPupil(client.getId()).stream().map(servicesMapper::getNameById).toList()))
                            .chatId(user.getId())
                            .parseMode("Markdown")
                            .build();
                    sender.send(message, user);
                });
    }

}
