package me.centralhardware.znatoki.telegram.statistic.telegram;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface Handler {

    void handle(Update update);

    boolean isAcceptable(String data);

    boolean isAcceptable(Update update);

}
