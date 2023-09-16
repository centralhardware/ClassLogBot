package me.centralhardware.znatoki.telegram.statistic.telegram.handler.studentCommand;

import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.service.SessionService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
import me.centralhardware.znatoki.telegram.statistic.utils.TelegramUtils;
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
public class SearchCommand extends CommandHandler {

    private final PupilService pupilService;
    private final TelegramUtils telegramUtils;
    private final SessionService sessionService;
    private final TelegramService telegramService;

    public SearchCommand(PupilService pupilService,
                         TelegramUtils telegramUtils,
                         SessionService sessionService,
                         TelegramService telegramService1) {
//        super("/s",
//                """
//                выполнить поиск ученика по текстовым полям. Аргументы: поисковый запрос. Пример:
//
//                <code> /s Михаил </code>
//                """, telegramService, statisticService);
        this.pupilService       = pupilService;
        this.telegramUtils      = telegramUtils;
        this.sessionService = sessionService;
        this.telegramService = telegramService1;
    }

    @Override
    public void handle(Message message) {
        if (!telegramUtils.checkReadAccess(message.getFrom(), "/s")) return;

        var arguments = message.getText().replace("/s", "").trim().split(" ");
        if (arguments.length == 1 && StringUtils.isBlank(arguments[0])){
            sender.sendText("Вы не ввели текст запроса. Пример: /s Иванов", message.getFrom());
            return;
        }

        String searchText = Arrays.toString(arguments).replace("]", "").replace("[", "");
        List<Pupil> searchResult = pupilService.search(searchText)
                .stream()
                .filter(not(Pupil::isDeleted))
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
