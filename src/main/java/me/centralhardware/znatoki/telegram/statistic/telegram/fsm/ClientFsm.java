package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Client;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServiceMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientFsm extends Fsm {

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
        var user = TelegramUtil.getFrom(update);
        var telegramUser = userMapper.getById(chatId);

        switch (storage.getCLientStage(chatId)){
            case ADD_FIO -> {
                String[] words = text.split(" ");
                if (!(words.length >= 2 && words.length <= 3)) {
                    sender.sendMessageFromResource(MessageConstant.INPUT_FIO_REQUIRED_FORMAT, user);
                    return;
                }
                Client client = storage.getClient(chatId);
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
                    getClient(chatId).setOrganizationId(userMapper.getById(chatId).getOrganizationId());
                    getClient(chatId).setCreated_by(chatId);
                    sender.sendMessageWithMarkdown(clientService.save(getClient(chatId)).getInfo(serviceMapper.getServicesForCLient(getClient(chatId).getId()).stream().map(servicesMapper::getNameById).toList()), user);
                    sendLog(getClient(chatId), chatId);
                    storage.remove(chatId);
                    sender.sendMessageFromResource(MessageConstant.CREATE_PUPIL_FINISHED, user);
                } else {
                    next(chatId);
                    storage.getClient(chatId).setPropertiesBuilder(new PropertiesBuilder(org.getClientCustomProperties().propertyDefs()));
                    var next = storage.getClient(chatId).getPropertiesBuilder().getNext().orElseThrow();
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
            case ADD_PROPERTIES -> processCustomProperties(update, getClient(chatId).getPropertiesBuilder(), properties -> {
                getClient(chatId).setOrganizationId(userMapper.getById(chatId).getOrganizationId());
                getClient(chatId).setCreated_by(chatId);
                getClient(chatId).setProperties(properties);
                sender.sendMessageWithMarkdown(clientService.save(getClient(chatId)).getInfo(serviceMapper.getServicesForCLient(getClient(chatId).getId()).stream().map(servicesMapper::getNameById).toList()), user);
                sendLog(getClient(chatId), chatId);
                storage.remove(chatId);
                sender.sendMessageFromResource(MessageConstant.CREATE_PUPIL_FINISHED, user);
            });
        }
    }

    public Client getClient(Long chatId){
        return storage.getClient(chatId);
    }

    public void next(Long chatId){
        var next = storage.getCLientStage(chatId).next();
        storage.setClientStage(chatId, next);
    }

    @Override
    public boolean isActive(Long chatId) {
        return storage.containsCLient(chatId);
    }

    private void sendLog(Client client, Long chatId) {
        getLogUser(chatId)
                .ifPresent(user -> {
                    var message = SendMessage.builder()
                            .text(STR."#\{organizationMapper.getById(client.getOrganizationId()).getClientName()}\n" + client.getInfo(serviceMapper.getServicesForCLient(client.getId()).stream().map(servicesMapper::getNameById).toList()))
                            .chatId(user.getId())
                            .parseMode("Markdown")
                            .build();
                    sender.send(message, user);
                });
    }

}
