package me.centralhardware.znatoki.telegram.statistic.validate;

import com.google.common.primitives.Ints;
import io.vavr.control.Either;
import org.springframework.stereotype.Component;

@Component
public class AmountValidator implements Validator<String, Integer> {

    private static final String NOT_NUMBER_ERROR = "Введенное значение должно быть числом";
    private static final String LESS_THAN_ZERO_ERROR = "Введенное значение должно быть больше нуля";

    @Override
    public Either<String, Integer> validate(String value) {
        Integer parsedValue = Ints.tryParse(value);
        if (parsedValue == null) {
            return Either.left(NOT_NUMBER_ERROR);
        }
        if (parsedValue <= 0) {
            return Either.left(LESS_THAN_ZERO_ERROR);
        }
        return Either.right(parsedValue);
    }
}
