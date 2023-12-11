package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.PaymentMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServiceMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.CallbackHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimeRestoreCallback  extends CallbackHandler {

    private final ServiceMapper serviceMapper;
    private final PaymentMapper paymentMapper;

    @Override
    public void handle(CallbackQuery callbackQuery, User from, String data) {
        if (!telegramService.isAdmin(from.getId())){
            sender.sendText("Доступ запрещен", from);
        }

        var id = UUID.fromString(data.replace("timeRestore-", ""));

        if (!serviceMapper.getOrgId(id).equals(getTelegramUser(from).getOrganizationId())){
            sender.sendText("Доступ запрещен", from);
            return;
        }

        serviceMapper.setDeleted(id, false);
        paymentMapper.setDeleteByTimeId(id, false);

        var editMessageReplyMarkup = EditMessageReplyMarkup
                .builder()
                .messageId(callbackQuery.getMessage().getMessageId())
                .chatId(callbackQuery.getMessage().getChatId())
                .replyMarkup(InlineKeyboardBuilder.create()
                        .setText("?")
                        .row()
                        .button("удалить", STR."timeDelete-\{id}")
                        .endRow().build())
                .build();
        sender.send(editMessageReplyMarkup, from);
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("timeRestore-");
    }
}
