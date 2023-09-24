package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.statisticCommand;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Payment;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps.AddPayment;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class AddPaymentCommand extends CommandHandler {

    private final Storage storage;

    @Override
    public void handle(Message message) {
        if (storage.contain(message.getChatId())){
            sender.sendText("Сначала сохраните текущую запись", message.getFrom(), false);
            return;
        }

        var payment = new Payment();
        payment.setChatId(message.getChatId());
        storage.setPayment(message.getChatId(), payment);
        storage.setPaymentStage(message.getChatId(),AddPayment.INPUT_PUPIL);
        sender.sendText("Введите фио", message.getFrom());
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.create()
                .row().switchToInline().endRow();
        builder.setText("нажмите для поиска фио");
        sender.send(builder.build(message.getChatId()), message.getFrom());
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/addPayment");
    }
}
