package me.centralhardware.znatoki.telegram.statistic.validate;

import com.google.common.primitives.Ints;
import io.vavr.control.Either;
import org.springframework.stereotype.Component;

@Component
public class AmountValidator implements Validator<String, Integer> {

    @Override
    public Either<String, Integer> validate(String value) {
        Integer val = Ints.tryParse(value);

        if (val == null){
            return Either.left("Введенное значение должно быть числом");
        }

        if (val <= 0){
            return Either.left("Введенное значение должно быть больше нуля");
        }

        return Either.right(val);
    }

}
