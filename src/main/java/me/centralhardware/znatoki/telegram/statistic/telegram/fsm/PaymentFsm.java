package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import org.telegram.telegrambots.meta.api.objects.Update;

public class PaymentFsm implements Fsm {
    @Override
    public void process(Update update) {

    }

    @Override
    public boolean isActive(Long chatId) {
        return false;
    }
}
