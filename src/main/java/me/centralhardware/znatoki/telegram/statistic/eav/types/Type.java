package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Objects;
import java.util.Optional;

public sealed interface Type permits Date, DateTime, Enumeration, Integer, Photo, Telephone, Text {

    String optionalText = "/skip для пропуска.";

    String format(String name, Boolean isOptional);
    Validation<String, Void> __validate(Update update, String...variants);

    Optional<String> extract(Update update);

    default Validation<String, Void> validate(Update update, String...variants){
        if (update.hasMessage() && Objects.equals(update.getMessage().getText(), "/skip")){
            return Validation.valid(null);
        }

        return __validate(update, variants);
    }

}
