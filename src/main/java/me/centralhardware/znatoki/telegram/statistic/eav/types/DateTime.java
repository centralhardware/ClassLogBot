package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public non-sealed class DateTime implements Type {

    private static final DateTimeFormatter dateFormat    = DateTimeFormatter.ofPattern("dd MM yyyy");

    @Override
    public String format(String name, Boolean isOptional) {
        return STR."Введите \{name} в формате dd MM yyyy \{isOptional? optionalText: ""}";
    }

    @Override
    public Validation<String, Void> __validate(Update update, String... variants) {
        try {
            LocalDate.parse(update.getMessage().getText(), dateFormat);
            return Validation.valid(null);
        } catch (Throwable e){
        }
        return Validation.invalid("Ошибка обработки даты необходимо ввести в формате: dd MM yyyy");
    }

    @Override
    public String extract(Update update) {
        return update.getMessage().getText();
    }

}
