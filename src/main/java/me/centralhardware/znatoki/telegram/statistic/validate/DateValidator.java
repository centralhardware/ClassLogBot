package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class DateValidator implements Validator<String, LocalDate>{

    private static final DateTimeFormatter dateFormat    = DateTimeFormatter.ofPattern("dd MM yyyy");

    @Override
    public Either<String, LocalDate> validate(String value) {
        try {
            return Either.right(LocalDate.parse(value, dateFormat));
        } catch (DateTimeException e){
            return Either.left("Ошибка обработки даты необходимо ввести в формате: dd MM yyyy");
        }
    }

}
