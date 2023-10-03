package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public non-sealed class Date implements Type {

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
    public Optional<String> extract(Update update) {
        return Optional.ofNullable(update.getMessage().getText());
    }

}
