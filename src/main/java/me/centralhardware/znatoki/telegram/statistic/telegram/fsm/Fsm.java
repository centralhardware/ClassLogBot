package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface Fsm {

    void process(Update update);
    boolean isActive(Long chatId);

}
