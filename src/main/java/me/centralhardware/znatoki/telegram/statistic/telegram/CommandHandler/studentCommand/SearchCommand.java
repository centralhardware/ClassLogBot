package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.studentCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Client;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.service.SessionService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Arrays;
import java.util.List;

import static java.util.function.Predicate.not;

/**
 * show user by text filed using full text search
 * param: search query
 * output format:
 * "fio
 * age л. classNumber кл.
 * button(информация) button(редактирование)
 * button(удалить) button(добавить оплату)
 * button(добавить занятие)"
 * input format: "/command/ search-query"
 * access level: read
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchCommand extends CommandHandler {

    private final ClientService clientService;
    private final TelegramUtil telegramUtils;
    private final SessionService sessionService;
    private final TelegramService telegramService;
    private final UserMapper userMapper;

    @Override
    public void handle(Message message) {
        if (!telegramUtils.checkReadAccess(message.getFrom(), sender)) return;

        var arguments = message.getText().replace("/s", "").trim().split(" ");
        if (arguments.length == 1 && StringUtils.isBlank(arguments[0])){
            sender.sendText("Вы не ввели текст запроса. Пример: /s Иванов", message.getFrom());
            return;
        }

        var orgId = userMapper.getById(message.getFrom().getId()).getOrganizationId();
        String searchText = Arrays.toString(arguments).replace("]", "").replace("[", "");
        List<Client> searchResult = clientService.search(searchText)
                .stream()
                .filter(not(Client::isDeleted))
                .filter(it -> it.getOrganizationId().equals(orgId))
                .toList();

        if (CollectionUtils.isEmpty(searchResult)){
            sender.sendMessageFromResource(MessageConstant.NOTHING_FOUND, message.getFrom());
            return;
        }

        sender.sendMessageFromResource(MessageConstant.SEARCH_RESULT, message.getFrom());
        for (Client client : searchResult) {
            String uuid = sessionService.create(client, message.getChatId());
            String link = String.format("%s/edit?sessionId=%s", Config.getBaseUrl(), uuid);
            InlineKeyboardBuilder inlineKeyboardBuilder = InlineKeyboardBuilder.
                    create().
                    setText(String.format("%s %s %s \n",
                            client.getName(),
                            client.getSecondName(),
                            client.getLastName())).
                    row().
                    button("информация", "/user_info" + client.getId()).
                    endRow();
            if (telegramService.hasWriteRight(message.getChatId())){
                inlineKeyboardBuilder.row().webApp(link, "редактировать").endRow();
            }
            inlineKeyboardBuilder.row().
                    button("удалить", "/delete_user" + client.getId()).
                    endRow();
            sender.send(inlineKeyboardBuilder.build(message.getChatId()), message.getFrom());
        }
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/s ");
    }
}
