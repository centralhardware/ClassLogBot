package me.centralhardware.znatoki.telegram.statistic.telegram.bulider;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardBuilder {

    private final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    private String text;
    private List<InlineKeyboardButton> row = null;

    private InlineKeyboardBuilder() { }

    public static InlineKeyboardBuilder create() {
        return new InlineKeyboardBuilder();
    }

    public InlineKeyboardBuilder setText( String text) {
        this.text = text;
        return this;
    }

    public InlineKeyboardBuilder row() {
        row = new ArrayList<>();
        return this;
    }

    public InlineKeyboardBuilder button( String text,  String callbackData) {
        row.add(InlineKeyboardButton.
                builder().
                text(text).
                callbackData(callbackData).
                build());
        return this;
    }

    public InlineKeyboardBuilder webApp(String url, String text){
        row.add(InlineKeyboardButton
                .builder()
                .text(text)
                .webApp(WebAppInfo
                        .builder()
                        .url(url)
                        .build())
                .build());
        return this;
    }

    public InlineKeyboardBuilder switchToInline(){
        row.add(InlineKeyboardButton
                .builder()
                .text("inline")
                .switchInlineQueryCurrentChat("")
                .build());
        return this;
    }

    public InlineKeyboardBuilder endRow() {
        keyboard.add(row);
        row = null;
        return this;
    }

    public SendMessage build(Long chatId) {
        return SendMessage.
                builder().
                chatId(chatId).
                text(text).
                replyMarkup(InlineKeyboardMarkup.
                        builder().
                        keyboard(keyboard).
                        build()).
                build();
    }


}
