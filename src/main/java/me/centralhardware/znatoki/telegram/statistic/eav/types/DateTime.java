package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public non-sealed class DateTime implements Type {

    private static final DateTimeFormatter dateFormat    = DateTimeFormatter.ofPattern("dd MM yyyy HH;mm");

    @Override
    public String format(String name, Boolean isOptional) {
        return STR."Введите \{name} в формате dd MM yyyy HH;mm \{isOptional? optionalText: ""}";
    }

    @Override
    public Validation<String, Void> __validate(Update update, String... variants) {
        return validate(update.getMessage().getText());
    }

    @Override
    public Validation<String, Void> validate(String value) {
        return Try.of(() -> LocalDate.parse(value, dateFormat))
                .fold(error -> Validation.invalid("Ошибка обработки даты необходимо ввести в формате: dd MM yyyy"),
                        date -> Validation.valid(null));
    }

    @Override
    public Optional<String> extract(Update update) {
        return Optional.ofNullable(update.getMessage().getText());
    }

    @Override
    public String getName() {
        return "DateTime";
    }
}
