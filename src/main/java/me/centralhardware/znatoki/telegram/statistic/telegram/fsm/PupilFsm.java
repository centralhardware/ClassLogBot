package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder;
import me.centralhardware.znatoki.telegram.statistic.entity.Client;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServiceMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
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
    private final Redis redis;

    private final ServiceMapper serviceMapper;
    private final ServicesMapper servicesMapper;
    private final OrganizationMapper organizationMapper;

    @Override
    public void process(Update update) {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        var user = update.getMessage().getFrom();
        var znatokiUser = redis.getUser(chatId);

        if (telegramService.isUnauthorized(chatId) || !telegramService.hasWriteRight(chatId)){
            sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, user);
            return;
        }

        switch (storage.getPupilStage(chatId)){
            case INPUT_FIO -> {
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

                var org = organizationMapper.getById(znatokiUser.get().organizationId());

                if (org.getClientCustomProperties() == null ||
                        org.getClientCustomProperties().isEmpty()){
                    getPupil(chatId).setOrganizationId(redis.getUser(chatId).get().organizationId());
                    getPupil(chatId).setCreated_by(chatId);
                    sender.sendMessageWithMarkdown(clientService.save(getPupil(chatId)).getInfo(serviceMapper.getServicesForPupil(getPupil(chatId).getId()).stream().map(servicesMapper::getNameById).toList()), user);
                    sendLog(getPupil(chatId), chatId);
                    storage.remove(chatId);
                    sender.sendMessageFromResource(MessageConstant.CREATE_PUPIL_FINISHED, user);
                } else {
                    next(chatId);
                    storage.getPupil(chatId).setPropertiesBuilder(new PropertiesBuilder(org.getServiceCustomProperties().propertyDefs()));
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
            case INPUT_PROPERTIES -> storage.getPupil(chatId).getPropertiesBuilder()
                    .validate(update)
                    .toEither()
                    .peekLeft(error -> sender.sendText(error, user))
                    .peek(it -> {
                        storage.getPupil(chatId).getPropertiesBuilder().setProperty(update);
                        storage.getPupil(chatId).getPropertiesBuilder().getNext()
                            .ifPresentOrElse(
                                    next -> {
                                        if (!next.getRight().isEmpty()){
                                            var builder = ReplyKeyboardBuilder
                                                    .create()
                                                    .setText(next.getLeft());
                                            next.getRight().forEach(variant -> builder.row()
                                                    .button(variant)
                                                    .endRow());
                                            sender.send(builder.build(chatId), user);
                                        } else {
                                            sender.sendText(next.getLeft(), user);
                                        }
                                    },
                                    () -> {
                                        getPupil(chatId).setOrganizationId(redis.getUser(chatId).get().organizationId());
                                        getPupil(chatId).setCreated_by(chatId);
                                        getPupil(chatId).setProperties(getPupil(chatId).getPropertiesBuilder().getProperties());
                                        sender.sendMessageWithMarkdown(clientService.save(getPupil(chatId)).getInfo(serviceMapper.getServicesForPupil(getPupil(chatId).getId()).stream().map(servicesMapper::getNameById).toList()), user);
                                        sendLog(getPupil(chatId), chatId);
                                        storage.remove(chatId);
                                        sender.sendMessageFromResource(MessageConstant.CREATE_PUPIL_FINISHED, user);
                                    });
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
                            .text("#ученик\n" + client.getInfo(serviceMapper.getServicesForPupil(client.getId()).stream().map(servicesMapper::getNameById).toList()))
                            .chatId(user.getId())
                            .parseMode("Markdown")
                            .build();
                    sender.send(message, user);
                });
    }

}
