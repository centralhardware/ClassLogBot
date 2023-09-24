package me.centralhardware.znatoki.telegram.statistic.telegram;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.PaymentMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CallbackHandler {

    private final TelegramService telegramService;
    private final PupilService pupilService;
    private final TelegramSender sender;
    private final Redis redis;

    private final TimeMapper timeMapper;
    private final PaymentMapper paymentMapper;
    private final ServicesMapper servicesMapper;

    public static final String USER_INFO_COMMAND        = "/user_info";
    public static final String DELETE_USER_COMMAND      = "/Љdelete_user";

    public boolean processCallback(Update update){
        if (!update.hasCallbackQuery()) return false;

        var callbackQuery = update.getCallbackQuery();
        var text = callbackQuery.getData();
        var from = callbackQuery.getFrom();
        var znatokiUser = redis.getUser(from.getId()).get();

        Long chatId = callbackQuery.getFrom().getId();
        if (text.startsWith(USER_INFO_COMMAND)){
            if (!telegramService.hasReadRight(chatId)){
                sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, from);
                return true;
            }
            var pupilOptional = pupilService.findById(Integer.parseInt(callbackQuery.getData().replace(USER_INFO_COMMAND,"")));
            pupilOptional.ifPresentOrElse(
                    pupil -> {
                        if (!pupil.getOrganizationId().equals(znatokiUser.organizationId())){
                            sender.sendText("Доступ запрещен", callbackQuery.getFrom());
                            return;
                        }

                        sender.sendMessageWithMarkdown(pupil.getInfo(timeMapper.getServicesForPupil(pupil.getId()).stream().map(servicesMapper::getNameById).toList()), callbackQuery.getFrom());
                    },
                    () -> sender.sendMessageFromResource(MessageConstant.USER_NOT_FOUND, callbackQuery.getFrom())
            );
        } else if (text.startsWith(DELETE_USER_COMMAND)){
            if (!telegramService.isAdmin(chatId)) {
                sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, from);
                return true;
            }

            pupilService.findById(Integer.parseInt(text.replace(DELETE_USER_COMMAND,""))).ifPresent(pupil -> {

                if (!pupil.getOrganizationId().equals(znatokiUser.organizationId())){
                    sender.sendText("Доступ запрещен", callbackQuery.getFrom());
                    return;
                }

                pupil.setDeleted(true);
                pupilService.save(pupil);
                sender.sendMessageFromResource(MessageConstant.PUPIL_DELETED, from);
            });
        } else if (text.startsWith("timeDelete-")){
            if (!telegramService.isAdmin(chatId)){
                sender.sendText("Доступ запрещен", callbackQuery.getFrom());
            }

            var id = UUID.fromString(text.replace("timeDelete-", ""));

            if (!timeMapper.getOrgId(id).equals(znatokiUser.organizationId())){
                sender.sendText("Доступ запрещен", callbackQuery.getFrom());
                return true;
            }

            timeMapper.setDeleted(id, true);
            paymentMapper.setDeleteByTimeId(id, true);

            var editMessageReplyMarkup = EditMessageReplyMarkup
                    .builder()
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .chatId(callbackQuery.getMessage().getChatId())
                    .replyMarkup(InlineKeyboardBuilder.create()
                            .row()
                            .button("восстановить", "timeRestore-" + id)
                            .endRow().build())
                    .build();
            sender.send(editMessageReplyMarkup, from);
        } else if (text.startsWith("timeRestore-")){
            if (!telegramService.isAdmin(chatId)){
                sender.sendText("Доступ запрещен", callbackQuery.getFrom());
            }

            var id = UUID.fromString(text.replace("timeRestore-", ""));

            if (!timeMapper.getOrgId(id).equals(znatokiUser.organizationId())){
                sender.sendText("Доступ запрещен", callbackQuery.getFrom());
                return true;
            }

            timeMapper.setDeleted(id, false);
            paymentMapper.setDeleteByTimeId(id, false);

            var editMessageReplyMarkup = EditMessageReplyMarkup
                    .builder()
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .chatId(callbackQuery.getMessage().getChatId())
                    .replyMarkup(InlineKeyboardBuilder.create()
                            .setText("?")
                            .row()
                            .button("удалить", "timeDelete-" + id)
                            .endRow().build())
                    .build();
            sender.send(editMessageReplyMarkup, from);
        }
        return true;
    }

}
