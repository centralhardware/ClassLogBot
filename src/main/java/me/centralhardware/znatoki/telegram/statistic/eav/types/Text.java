package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

public non-sealed class Text implements Type {
    @Override
    public String format(String name, Boolean isOptional) {
        return STR."Введите \{name}. \{isOptional? optionalText: ""}";
    }

    @Override
    public Validation<String, Void> __validate(Update update, String...variants) {
        return update.hasMessage() && StringUtils.isNotBlank(update.getMessage().getText()) ?
                Validation.valid(null) :
                Validation.invalid("Введите текст");
    }

    @Override
    public Optional<String> extract(Update update) {
        return Optional.ofNullable(update.getMessage().getText());
    }
}
