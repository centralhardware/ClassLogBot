package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.Optional;

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
    public Validation<String, Void> validate(String value) {
        return null;
    }

    @Override
    public Optional<String> extract(Update update) {
        return Optional.ofNullable(update.getMessage().getText());
    }

    @Override
    public String getName() {
        return "Enumeration";
    }

}
