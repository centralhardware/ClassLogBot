package me.centralhardware.znatoki.telegram.statistic.telegram.handler.studentCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.service.SessionService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Arrays;
import java.util.List;

import static java.util.function.Predicate.not;
import static me.centralhardware.znatoki.telegram.statistic.telegram.CallbackHandler.DELETE_USER_COMMAND;
import static me.centralhardware.znatoki.telegram.statistic.telegram.CallbackHandler.USER_INFO_COMMAND;

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

    private final PupilService pupilService;
    private final TelegramUtil telegramUtils;
    private final SessionService sessionService;
    private final TelegramService telegramService;
    private final Redis redis;

    @Override
    public void handle(Message message) {
        if (!telegramUtils.checkReadAccess(message.getFrom(), "/s", sender)) return;

        var arguments = message.getText().replace("/s", "").trim().split(" ");
        if (arguments.length == 1 && StringUtils.isBlank(arguments[0])){
            sender.sendText("Вы не ввели текст запроса. Пример: /s Иванов", message.getFrom());
            return;
        }

        var orgId = redis.getUser(message.getFrom().getId()).get().organizationId();
        String searchText = Arrays.toString(arguments).replace("]", "").replace("[", "");
        List<Pupil> searchResult = pupilService.search(searchText)
                .stream()
                .filter(not(Pupil::isDeleted))
                .filter(it -> it.getOrganizationId().equals(orgId))
                .toList();

        if (CollectionUtils.isEmpty(searchResult)){
            sender.sendMessageFromResource(MessageConstant.NOTHING_FOUND, message.getFrom());
            return;
        }

        sender.sendMessageFromResource(MessageConstant.SEARCH_RESULT, message.getFrom());
        for (Pupil pupil : searchResult) {
            String uuid = sessionService.create(pupil, message.getChatId());
            String link = String.format("%s/edit?sessionId=%s", Config.getBaseUrl(), uuid);
            InlineKeyboardBuilder inlineKeyboardBuilder = InlineKeyboardBuilder.
                    create().
                    setText(String.format("%s %s %s \n%s л. %s кл.",
                            pupil.getName(),
                            pupil.getSecondName(),
                            pupil.getLastName(),
                            pupil.getAge(),
                            pupil.getClassNumber())).
                    row().
                    button("информация", USER_INFO_COMMAND + pupil.getId()).
                    endRow();
            if (telegramService.hasWriteRight(message.getChatId())){
                inlineKeyboardBuilder.row().webApp(link, "редактировать").endRow();
            }
            inlineKeyboardBuilder.row().
                    button("удалить", DELETE_USER_COMMAND + pupil.getId()).
                    endRow();
            sender.send(inlineKeyboardBuilder.build(message.getChatId()), message.getFrom());
        }
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/s ");
    }
}
