package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import org.telegram.telegrambots.meta.api.objects.Update;

public non-sealed class Photo implements Type {
    @Override
    public String format(String name, Boolean isOptional) {
        return STR."Отправьте фото \{name}. \{isOptional? optionalText: ""}";
    }

    @Override
    public Validation<String, Void> __validate(Update update, String...variants) {
        return update.hasMessage() && update.getMessage().hasPhoto() ?
                Validation.valid(null) :
                Validation.invalid("Отправьте фото");
    }

    @Override
    public String extract(Update update) {
        return update.getMessage().getText();
    }

}
