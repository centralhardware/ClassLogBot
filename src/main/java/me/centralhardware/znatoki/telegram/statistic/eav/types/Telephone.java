package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import me.centralhardware.znatoki.telegram.statistic.utils.TelephoneUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

public non-sealed class Telephone implements Type{
    @Override
    public String format(String name, Boolean isOptional) {
        return STR."Введите телефон \{name}. \{isOptional? optionalText: ""}";
    }

    @Override
    public Validation<String, Void> __validate(Update update, String... variants) {
        return update.hasMessage() && TelephoneUtils.validate(update.getMessage().getText())?
                Validation.valid(null) :
                Validation.invalid("Введите номер телефона");
    }

    @Override
    public String extract(Update update) {
        return update.getMessage().getText();
    }

}
