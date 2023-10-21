package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

public non-sealed class Number implements Type {
    @Override
    public String format(String name, Boolean isOptional) {
        return STR."Введите \{name} (число). \{isOptional? optionalText: ""}";
    }

    @Override
    public Validation<String, Void> __validate(Update update, String...variants) {
        return validate(Optional.ofNullable(update).map(Update::getMessage).map(Message::getText).orElse(null));
    }

    @Override
    public Validation<String, Void> validate(String value) {
        return StringUtils.isNumeric(value) ?
                Validation.valid(null) :
                Validation.invalid("Введите число");
    }

    @Override
    public Optional<String> extract(Update update) {
        return Optional.ofNullable(update.getMessage().getText());
    }

    @Override
    public String getName() {
        return "Integer";
    }

}
