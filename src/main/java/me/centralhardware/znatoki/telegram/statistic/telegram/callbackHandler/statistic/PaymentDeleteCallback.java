package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.PaymentMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.CallbackHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class PaymentDeleteCallback extends CallbackHandler {

    private final PaymentMapper paymentMapper;

    @Override
    public void handle(CallbackQuery callbackQuery, User from, String data) {
        if (!telegramService.isAdmin(from.getId())){
            sender.sendText("Доступ запрещен", from);
        }

        var id = Integer.parseInt(data.replace("paymentDelete-", ""));

        if (!paymentMapper.getOrgById(id).equals(getTelegramUser(from).getOrganizationId())){
            sender.sendText("Доступ запрещен", from);
            return;
        }

        paymentMapper.setDelete(id, true);

        var editMessageReplyMarkup = EditMessageReplyMarkup
                .builder()
                .messageId(Integer.valueOf(callbackQuery.getInlineMessageId()))
                .chatId(callbackQuery.getMessage().getChatId())
                .replyMarkup(InlineKeyboardBuilder.create()
                        .row()
                        .button("восстановить", STR."paymentRestore-\{id}")
                        .endRow().build())
                .build();
        sender.send(editMessageReplyMarkup, from);
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("paymentDelete-");
    }
}
