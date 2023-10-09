package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import me.centralhardware.znatoki.telegram.statistic.utils.TelephoneUtils;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

public non-sealed class Telephone implements Type{
    @Override
    public String format(String name, Boolean isOptional) {
        return STR."Введите телефон \{name}. \{isOptional? optionalText: ""}";
    }

    @Override
    public Validation<String, Void> __validate(Update update, String... variants) {
        return validate(Optional.ofNullable(update).map(Update::getMessage).map(Message::getText).orElse(null));
    }

    @Override
    public Validation<String, Void> validate(String value) {
        return TelephoneUtils.validate(value)?
                Validation.valid(null) :
                Validation.invalid("Введите номер телефона");
    }

    @Override
    public Optional<String> extract(Update update) {
        return Optional.ofNullable(update.getMessage().getText());
    }

    @Override
    public String getName() {
        return "Telephone";
    }

}
