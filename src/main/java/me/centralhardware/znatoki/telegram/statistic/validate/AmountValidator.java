package me.centralhardware.znatoki.telegram.statistic.validate;

import com.google.common.primitives.Ints;
import io.vavr.control.Either;
import org.springframework.stereotype.Component;

@Component
public class AmountValidator implements Validator<String, Integer> {

    @Override
    public Either<Integer, String> validate(String value) {
        Integer val = Ints.tryParse(value);

        if (val == null){
            return Either.right("Введенное значение должно быть числом");
        }

        if (val <= 0){
            return Either.right("Введенное значение должно быть больше нуля");
        }

        return Either.left(val);
    }

}
