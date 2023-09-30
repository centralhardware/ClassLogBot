package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;

public non-sealed class Enumeration implements Type {
    @Override
    public String format(String name, Boolean isOptional) {
        return STR."Выберите \{name}. \{isOptional? optionalText: ""}";
    }

    @Override
    public Validation<String, Void> __validate(Update update, String...variants) {
        return update.hasMessage() && Arrays.asList(variants).contains(update.getMessage().getText())?
                Validation.valid(null) :
                Validation.invalid("Выберите вариант из кастомный клавиатуры");
    }

    @Override
    public String extract(Update update) {
        return update.getMessage().getText();
    }

}
